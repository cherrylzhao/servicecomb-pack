/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.omega.connector.grpc.core;

import com.google.common.base.Optional;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLException;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.saga.GrpcSagaClientMessageSender;
import org.apache.servicecomb.pack.omega.connector.grpc.tcc.GrpcTccClientMessageSender;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;

public class LoadBalanceContextFactory {

  public static LoadBalanceContext newInstance(TransactionType transactionType,
      AlphaClusterConfig clusterConfig, ServiceConfig serviceConfig, long reconnectDelay, long timeoutSeconds) {
    if (clusterConfig.getAddresses().isEmpty()) {
      throw new IllegalArgumentException("No reachable cluster address provided");
    }
    Optional<SslContext> sslContext = buildSslContext(clusterConfig);
    Map<MessageSender, Long> senders = new ConcurrentHashMap<>();
    Collection<ManagedChannel> channels = new ArrayList<>(clusterConfig.getAddresses().size());
    LoadBalanceContext loadContext = new LoadBalanceContext(senders, channels);
    for (String address : clusterConfig.getAddresses()) {
      ManagedChannel channel = buildChannel(address, sslContext);
      channels.add(channel);
      MessageSender messageSender = buildSender(transactionType, address, channel, clusterConfig, serviceConfig);
      senders.put(messageSender, 0L);
    }
    ErrorHandleEngineManager.init(senders, reconnectDelay, timeoutSeconds);
    return loadContext;
  }

  private static ManagedChannel buildChannel(final String address, final Optional<SslContext> sslContext) {
    if (sslContext.isPresent()) {
      return NettyChannelBuilder.forTarget(address)
          .negotiationType(NegotiationType.TLS)
          .sslContext(sslContext.get())
          .build();
    } else {
      return ManagedChannelBuilder
          .forTarget(address).usePlaintext()
          .build();
    }
  }

  private static MessageSender buildSender(final TransactionType transactionType, final String address, final ManagedChannel channel,
      final AlphaClusterConfig clusterConfig, final ServiceConfig serviceConfig) {
    switch (transactionType) {
      case TCC:
        return new GrpcTccClientMessageSender(serviceConfig, channel, address);
      case SAGA:
        return new GrpcSagaClientMessageSender(
            address,
            channel,
            clusterConfig.getMessageSerializer(),
            clusterConfig.getMessageDeserializer(),
            serviceConfig
        );
        default:
    }
      return null;
  }

  private static Optional<SslContext> buildSslContext(final AlphaClusterConfig clusterConfig) {
    if (!clusterConfig.isEnableSSL()) {
      return Optional.absent();
    }

    SslContextBuilder builder = GrpcSslContexts.forClient();
    // openssl must be used because some older JDk does not support cipher suites required by http2,
    // and the performance of JDK ssl is pretty low compared to openssl.
    builder.sslProvider(SslProvider.OPENSSL);

    Properties prop = new Properties();
    try {
      prop.load(LoadBalanceContextFactory.class.getClassLoader().getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read ssl.properties.", e);
    }

    builder.protocols(prop.getProperty("protocols").split(","));
    builder.ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    builder.trustManager(new File(clusterConfig.getCertChain()));

    if (clusterConfig.isEnableMutualAuth()) {
      builder.keyManager(new File(clusterConfig.getCert()), new File(clusterConfig.getKey()));
    }

    try {
      return Optional.of(builder.build());
    } catch (SSLException e) {
      throw new IllegalArgumentException("Unable to build SslContext", e);
    }
  }
}

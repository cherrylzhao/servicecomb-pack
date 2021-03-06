/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.pack.omega.transaction.tcc;

import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationStartedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccStartedEvent;

public interface TccMessageSender extends MessageSender {

  AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent);

  AlphaResponse participationStart(ParticipationStartedEvent participationStartedEvent);

  AlphaResponse participationEnd(ParticipationEndedEvent participationEndedEvent);

  AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent);

  AlphaResponse coordinate(CoordinatedEvent coordinatedEvent);

}

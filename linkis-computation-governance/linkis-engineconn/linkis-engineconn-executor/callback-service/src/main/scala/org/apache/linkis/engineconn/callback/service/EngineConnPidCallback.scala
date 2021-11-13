/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.linkis.engineconn.callback.service

import java.lang.management.ManagementFactory

import org.apache.linkis.common.ServiceInstance
import org.apache.linkis.engineconn.core.EngineConnObject
import org.apache.linkis.governance.common.protocol.task.ResponseEngineConnPid
import org.apache.linkis.rpc.Sender


class EngineConnPidCallback(emInstance: ServiceInstance) extends AbstractEngineConnStartUpCallback(emInstance) {

  override def callback(): Unit = {
    val pid = ManagementFactory.getRuntimeMXBean.getName.split("@")(0)
    val instance = Sender.getThisServiceInstance
    val context = EngineConnObject.getEngineCreationContext
    callback(ResponseEngineConnPid(instance, pid, context.getTicketId))
  }

}

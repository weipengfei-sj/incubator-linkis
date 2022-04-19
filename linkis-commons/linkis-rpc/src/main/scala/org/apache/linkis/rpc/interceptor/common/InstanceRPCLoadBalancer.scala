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
 
package org.apache.linkis.rpc.interceptor.common

import com.netflix.loadbalancer.ILoadBalancer
import org.apache.linkis.common.ServiceInstance
import org.apache.linkis.protocol.{InstanceProtocol, Protocol}
import org.apache.linkis.rpc.interceptor.RPCLoadBalancer
import org.springframework.stereotype.Component


@Component
class InstanceRPCLoadBalancer extends RPCLoadBalancer {
  override val order: Int = 10

  override def choose(protocol: Protocol, originService: ServiceInstance, lb: ILoadBalancer): Option[ServiceInstance] = protocol match {
    case instance: InstanceProtocol =>
      instance.choseInstance.map(ServiceInstance(originService.getApplicationName, _))
    case _ => None
  }
}
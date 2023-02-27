/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.manager.common.entity.node;

import org.apache.linkis.common.ServiceInstance;
import org.apache.linkis.manager.common.entity.enumeration.NodeStatus;
import org.apache.linkis.manager.common.entity.resource.NodeResource;

import java.util.Date;

public class InfoRMNode implements RMNode {

  private ServiceInstance serviceInstance;

  private NodeResource nodeResource;

  private String owner;

  private String mark;

  private NodeStatus nodeStatus;

  private Date startTime;

  private Date updateTime;

  @Override
  public NodeResource getNodeResource() {
    return nodeResource;
  }

  @Override
  public void setNodeResource(NodeResource nodeResource) {
    this.nodeResource = nodeResource;
  }

  @Override
  public ServiceInstance getServiceInstance() {
    return serviceInstance;
  }

  @Override
  public void setServiceInstance(ServiceInstance serviceInstance) {
    this.serviceInstance = serviceInstance;
  }

  @Override
  public NodeStatus getNodeStatus() {
    return nodeStatus;
  }

  @Override
  public void setNodeStatus(NodeStatus status) {
    this.nodeStatus = status;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public String getMark() {
    return mark;
  }

  @Override
  public Date getUpdateTime() {
    return updateTime;
  }

  @Override
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public Date getStartTime() {
    return startTime;
  }

  @Override
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }
}

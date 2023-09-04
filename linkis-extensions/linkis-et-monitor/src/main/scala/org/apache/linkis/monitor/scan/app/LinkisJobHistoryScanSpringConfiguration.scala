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

package org.apache.linkis.monitor.scan.app

import org.apache.linkis.monitor.scan.app.factory.MapperFactory
import org.apache.linkis.monitor.scan.app.instance.dao.InstanceInfoDao
import org.apache.linkis.monitor.scan.app.jobhistory.dao.JobHistoryMapper

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{ComponentScan, Configuration}

import javax.annotation.PostConstruct

/**
 * Created by shangda on 2021/11/19.
 */

@Configuration
@ComponentScan(Array("org.apache.linkis.monitor.scan", "org.apache.linkis.mybatis"))
class LinkisJobHistoryScanSpringConfiguration {

  @Autowired
  private var jobHistoryMapper: JobHistoryMapper = _

  @Autowired
  private var instanceInfoMapper: InstanceInfoDao = _

  @PostConstruct
  def init(): Unit = {
    MapperFactory.setJobHistoryMapper(jobHistoryMapper)
    MapperFactory.setInstanceInfoMapper(instanceInfoMapper)
  }

}

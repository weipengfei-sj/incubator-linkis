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

package org.apache.linkis.monitor.scan.app.jobhistory.labels

import java.util

import com.google.common.collect.HashBiMap
import org.apache.commons.lang3.StringUtils
import org.apache.linkis.common.utils.Logging
import org.apache.linkis.monitor.scan.app.jobhistory.entity.JobHistory
import org.apache.linkis.monitor.scan.app.monitor.until.CacheUtils
import org.apache.linkis.monitor.scan.constants.Constants
import org.apache.linkis.monitor.scan.core.ob.Observer
import org.apache.linkis.monitor.scan.core.pac.{AbstractScanRule, ScannedData}
import org.apache.linkis.server.BDPJettyServerHelper

import scala.collection.JavaConverters._

/**
 * 对前20分钟内的执行数据进行扫描，对数据的labels字段进行判断,
 * 判断依据monitor配置（linkis.monitor.jobhistory.userLabel.tenant）
 */
class JobHistoryLabelsRule(hitObserver: Observer)
    extends AbstractScanRule(event = new JobHistoryLabelsHitEvent, observer = hitObserver)
    with Logging {

  private val scanRuleList = CacheUtils.cacheBuilder

  /**
   * if data match the pattern, return true and trigger observer should call isMatched()
   *
   * @param data
   * @return
   */
  override def triggerIfMatched(data: util.List[ScannedData]): Boolean = {
    if (!getHitEvent.isRegistered || null == data) {
      logger.error("ScanRule is not bind with an observer. Will not be triggered")
      return false
    }
    val alertData: util.List[JobHistory] = new util.ArrayList[JobHistory]()
    for (sd <- data.asScala) {
      if (sd != null && sd.getData() != null) {
        for (d <- sd.getData().asScala) {
          if (d.isInstanceOf[JobHistory]) {
            logger.info(" start jobhistory user label rule data : {}", d)
            val jobHistory = d.asInstanceOf[JobHistory]
            val labels = jobHistory.getLabels
            val labelsMap: util.Map[String, String] =
              BDPJettyServerHelper.gson.fromJson(labels, classOf[java.util.Map[String, String]])
            val userCreator = labelsMap.get("userCreator");
            val tenant = labelsMap.get("tenant");
            if (StringUtils.isNotBlank(userCreator)) {
              val configMap = BDPJettyServerHelper.gson.fromJson(
                Constants.USER_LABEL_TENANT.getValue,
                classOf[java.util.Map[String, String]]
              )
              // 当任务的creator是qualitis（或dops）时，tenant不是qualitis发出告警
              val listIterator = configMap.keySet.iterator
              while ({
                listIterator.hasNext
              }) {
                val next = listIterator.next
                if (userCreator.contains(next)) {
                  val value = configMap.get(next)
                  if (!value.equals(tenant)) {
                    alertData.add(d.asInstanceOf[JobHistory])
                  }
                }
              }
              // 当任务代理tenant:Qualitis标签，但是creator不是qualitis标签也进行告警
              if (configMap.values().contains(tenant)) {
                val bimap: HashBiMap[String, String] = HashBiMap.create(configMap)
                val key = bimap.inverse().get(tenant)
                if (!key.contains(userCreator)) {
                  alertData.add(d.asInstanceOf[JobHistory])
                }
              }
            }
            scanRuleList.put("jobHistoryId", jobHistory.getId)
          } else {
            logger.warn("Ignored wrong input data Type : " + d + ", " + d.getClass.getCanonicalName)
          }
        }
      } else {
        logger.warn("Ignored null scanned data")
      }
    }
    logger.info("hit " + alertData.size() + " data in one iteration")
    if (alertData.size() > 0) {
      getHitEvent.notifyObserver(getHitEvent, alertData)
       true
    } else {
       false
    }
  }

}

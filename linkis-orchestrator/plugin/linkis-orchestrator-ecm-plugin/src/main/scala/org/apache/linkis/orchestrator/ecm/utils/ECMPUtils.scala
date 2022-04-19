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

package org.apache.linkis.orchestrator.ecm.utils

import java.util

import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.entrance.JobStrategyLabel
import org.apache.linkis.manager.label.utils.LabelUtils

import scala.collection.JavaConverters._

object ECMPUtils {

  def filterJobStrategyLabel(labels: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
    if (null == labels || labels.isEmpty) {
      return labels
    }
    val list: util.List[Label[_]] = LabelBuilderFactoryContext.getLabelBuilderFactory.getLabels(labels)
    LabelUtils.labelsToMap(filterJobStrategyLabel(list))
  }


  def filterJobStrategyLabel(labels: util.List[Label[_]]): util.List[Label[_]] = {
    if (null == labels || labels.isEmpty) {
      return labels
    }
    labels.asScala.filter(! _.isInstanceOf[JobStrategyLabel]).asJava
  }


}

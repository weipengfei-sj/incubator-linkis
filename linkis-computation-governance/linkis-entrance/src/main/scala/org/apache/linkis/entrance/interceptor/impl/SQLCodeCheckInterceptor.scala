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
 
package org.apache.linkis.entrance.interceptor.impl

import org.apache.linkis.entrance.interceptor.EntranceInterceptor
import org.apache.linkis.entrance.interceptor.exception.CodeCheckException
import org.apache.linkis.governance.common.entity.job.JobRequest
import org.apache.linkis.manager.label.utils.LabelUtil


class SQLCodeCheckInterceptor extends EntranceInterceptor {

  override def apply(jobRequest: JobRequest, logAppender: java.lang.StringBuilder): JobRequest = {
    val engineType = LabelUtil.getEngineType(jobRequest.getLabels)
    engineType.toLowerCase() match {
        case "hql" | "sql" | "jdbc" | "hive" =>
          val sb: StringBuilder = new StringBuilder
        val isAuth: Boolean = SQLExplain.authPass(jobRequest.getExecutionCode, sb)
          if (!isAuth) {
            throw CodeCheckException(20051, "sql code check failed, reason is " + sb.toString())
          }
        case _ =>
      }
    jobRequest

  }
}

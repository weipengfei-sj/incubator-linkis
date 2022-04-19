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

package org.apache.linkis.datasource.client.request

import org.apache.linkis.datasource.client.config.DatasourceClientConfig.METADATA_SERVICE_MODULE
import org.apache.linkis.datasource.client.exception.DataSourceClientBuilderException
import org.apache.linkis.httpclient.request.GetAction


class MetadataGetDatabasesAction extends GetAction with DataSourceAction {
  private var dataSourceId: Long = _

  override def suffixURLs: Array[String] = Array(METADATA_SERVICE_MODULE.getValue, "dbs", dataSourceId.toString)

  private var user: String = _

  override def setUser(user: String): Unit = this.user = user

  override def getUser: String = this.user
}

object MetadataGetDatabasesAction {
  def builder(): Builder = new Builder

  class Builder private[MetadataGetDatabasesAction]() {
    private var dataSourceId: Long = _
    private var system: String = _
    private var user: String = _

    def setUser(user: String): Builder = {
      this.user = user
      this
    }

    def setDataSourceId(dataSourceId: Long): Builder = {
      this.dataSourceId = dataSourceId
      this
    }

    def setSystem(system: String): Builder = {
      this.system = system
      this
    }

    def build(): MetadataGetDatabasesAction = {
      if (dataSourceId == null) throw new DataSourceClientBuilderException("dataSourceId is needed!")
      if(system == null) throw new DataSourceClientBuilderException("system is needed!")
      if(user == null) throw new DataSourceClientBuilderException("user is needed!")

      val metadataGetDatabasesAction = new MetadataGetDatabasesAction
      metadataGetDatabasesAction.dataSourceId = this.dataSourceId
      metadataGetDatabasesAction.setParameter("system", system)
      metadataGetDatabasesAction.setUser(user)
      metadataGetDatabasesAction
    }
  }

}

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

package org.apache.linkis.datasource.client.request

import org.apache.linkis.datasource.client.config.DatasourceClientConfig.METADATA_SERVICE_MODULE
import org.apache.linkis.datasource.client.exception.DataSourceClientBuilderException
import org.apache.linkis.httpclient.request.GetAction

class MetadataGetPartitionsAction extends GetAction with DataSourceAction {
  private var dataSourceName: String = _
  private var database: String = _
  private var table: String = _
  private var traverse: Boolean = false

  override def suffixURLs: Array[String] =
    Array(METADATA_SERVICE_MODULE.getValue, "getPartitions")

  private var user: String = _

  override def setUser(user: String): Unit = this.user = user

  override def getUser: String = this.user
}

object MetadataGetPartitionsAction {
  def builder(): Builder = new Builder

  class Builder private[MetadataGetPartitionsAction] () {
    private var dataSourceName: String = _
    private var database: String = _
    private var table: String = _
    private var system: String = _
    private var user: String = _
    private var traverse: Boolean = false

    def setUser(user: String): Builder = {
      this.user = user
      this
    }

    def setDataSourceName(dataSourceName: String): Builder = {
      this.dataSourceName = dataSourceName
      this
    }

    def setDatabase(database: String): Builder = {
      this.database = database
      this
    }

    def setTable(table: String): Builder = {
      this.table = table
      this
    }

    def setTraverse(traverse: Boolean): Builder = {
      this.traverse = traverse
      this
    }

    def setSystem(system: String): Builder = {
      this.system = system
      this
    }

    def build(): MetadataGetPartitionsAction = {
      if (dataSourceName == null)
        throw new DataSourceClientBuilderException("dataSourceName is needed!")
      if (database == null) throw new DataSourceClientBuilderException("database is needed!")
      if (table == null) throw new DataSourceClientBuilderException("table is needed!")
      if (system == null) throw new DataSourceClientBuilderException("system is needed!")

      val metadataGetPartitionsAction = new MetadataGetPartitionsAction
      metadataGetPartitionsAction.dataSourceName = this.dataSourceName
      metadataGetPartitionsAction.database = this.database
      metadataGetPartitionsAction.table = this.table
      metadataGetPartitionsAction.setParameter("system", system)
      metadataGetPartitionsAction.setUser(user)
      metadataGetPartitionsAction.traverse = this.traverse
      metadataGetPartitionsAction
    }

  }

}

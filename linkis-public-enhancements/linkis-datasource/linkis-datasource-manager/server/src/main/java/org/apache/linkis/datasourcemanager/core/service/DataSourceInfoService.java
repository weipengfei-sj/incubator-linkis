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

package org.apache.linkis.datasourcemanager.core.service;

import org.apache.linkis.common.exception.ErrorException;
import org.apache.linkis.datasourcemanager.common.domain.DataSource;
import org.apache.linkis.datasourcemanager.common.domain.DataSourceEnv;
import org.apache.linkis.datasourcemanager.common.domain.DataSourceParamKeyDefinition;
import org.apache.linkis.datasourcemanager.common.domain.DatasourceVersion;
import org.apache.linkis.datasourcemanager.core.vo.DataSourceEnvVo;
import org.apache.linkis.datasourcemanager.core.vo.DataSourceVo;

import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface DataSourceInfoService {

    /**
     * Save data source information
     *
     * @param dataSource data source
     */
    void saveDataSourceInfo(DataSource dataSource) throws ErrorException;

    /**
     * Add parameters of data source environment
     *
     * @param dataSourceEnvId data source environment
     * @param dataSource data source
     */
    void addEnvParamsToDataSource(Long dataSourceEnvId, DataSource dataSource);

    /**
     * Get data source for current version
     *
     * @param dataSourceId id
     * @return data source entity
     */
    DataSource getDataSourceInfo(Long dataSourceId);

    /**
     * Get data source for current version by data source name
     *
     * @param dataSourceName data source name
     * @return data source entity
     */
    DataSource getDataSourceInfo(String dataSourceName);
    /**
     * Get data source
     *
     * @param dataSourceId id
     * @return data source entity
     */
    DataSource getDataSourceInfo(Long dataSourceId, Long version);

    /**
     * Get data source brief information
     *
     * @param dataSourceId data source id
     * @return
     */
    DataSource getDataSourceInfoBrief(Long dataSourceId);
    /**
     * Remove data source
     *
     * @param dataSourceId id
     * @param createSystem system name
     * @return
     */
    Long removeDataSourceInfo(Long dataSourceId, String createSystem);

    /**
     * Update data source
     *
     * @param updatedOne updated data source
     */
    void updateDataSourceInfo(DataSource updatedOne) throws ErrorException;

    /**
     * Page query of data source
     *
     * @param dataSourceVo data source view entity
     * @return
     */
    PageInfo<DataSource> queryDataSourceInfoPage(DataSourceVo dataSourceVo);

    /**
     * Save data source environment
     *
     * @param dataSourceEnv data source environment
     */
    void saveDataSourceEnv(DataSourceEnv dataSourceEnv) throws ErrorException;

    /**
     * List data source environments
     *
     * @param dataSourceTypeId type id
     * @return
     */
    List<DataSourceEnv> listDataSourceEnvByType(Long dataSourceTypeId);

    /**
     * Get data source environment
     *
     * @param envId environment id
     * @return
     */
    DataSourceEnv getDataSourceEnv(Long envId);

    /**
     * Remove data source environment
     *
     * @param envId environment id
     * @return
     */
    Long removeDataSourceEnv(Long envId);

    /**
     * Update data source environment
     *
     * @param updatedOne
     * @param storedOne
     */
    void updateDataSourceEnv(DataSourceEnv updatedOne, DataSourceEnv storedOne)
            throws ErrorException;

    /**
     * Page query of data source environment
     *
     * @param dataSourceEnvVo
     * @return
     */
    List<DataSourceEnv> queryDataSourceEnvPage(DataSourceEnvVo dataSourceEnvVo);

    /**
     * exoire data source
     *
     * @param dataSourceId
     * @return
     */
    Long expireDataSource(Long dataSourceId);

    /**
     * publish datasource by id
     *
     * @param dataSourceId @Param versionId
     * @return
     */
    int publishByDataSourceId(Long dataSourceId, Long versionId);

    /**
     * insert a datasource parameter, return new version
     *
     * @param keyDefinitionList
     * @param datasourceId
     * @param connectParams
     * @param comment @Param username
     * @return
     */
    long insertDataSourceParameter(
            List<DataSourceParamKeyDefinition> keyDefinitionList,
            Long datasourceId,
            Map<String, Object> connectParams,
            String username,
            String comment)
            throws ErrorException;

    /**
     * get datasource version list
     *
     * @param datasourceId
     * @return
     */
    List<DatasourceVersion> getVersionList(Long datasourceId);

    /**
     * get datasource info for connect for published version, if there is a dependency environment,
     * merge datasource parameter and environment parameter.
     *
     * @param dataSourceId
     * @return
     */
    DataSource getDataSourceInfoForConnect(Long dataSourceId);

    /**
     * get datasource info for connect for published version by name, if there is a dependency
     * environment, merge datasource parameter and environment parameter.
     *
     * @param dataSourceName
     * @return
     */
    DataSource getDataSourceInfoForConnect(String dataSourceName);
    /**
     * get datasource info for connect, if there is a dependency environment, merge datasource
     * parameter and environment parameter.
     *
     * @param dataSourceId
     * @param version
     * @return
     */
    DataSource getDataSourceInfoForConnect(Long dataSourceId, Long version);

    /**
     * Check if exist data source
     *
     * @param dataSourceName data source name
     * @return boolean
     */
    boolean existDataSource(String dataSourceName);
}

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

package org.apache.linkis.metadata.query.server.restful;

import org.apache.linkis.common.exception.ErrorException;
import org.apache.linkis.datasourcemanager.common.util.json.Json;
import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;
import org.apache.linkis.metadata.query.common.domain.MetaPartitionInfo;
import org.apache.linkis.metadata.query.common.exception.MetaMethodInvokeException;
import org.apache.linkis.metadata.query.server.service.MetadataQueryService;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Api(tags = "metadata query")
@RestController
@RequestMapping(value = "/metadataQuery")
public class MetadataQueryRestful {

    private static final Logger logger = LoggerFactory.getLogger(MetadataQueryRestful.class);

    @Autowired private MetadataQueryService metadataQueryService;

    @ApiOperation(value = "getDatabases", notes = "get databases", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "system")
    })
    @RequestMapping(value = "/getDatabases", method = RequestMethod.GET)
    public Message getDatabases(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("system") String system,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }

            List<String> databases =
                    metadataQueryService.getDatabasesByDsName(
                            dataSourceName, system, SecurityFilter.getLoginUsername(request));
            return Message.ok().data("dbs", databases);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get database list[获取库信息失败], name:["
                            + dataSourceName
                            + "], system:["
                            + system
                            + "]",
                    e);
        }
    }

    @ApiOperation(value = "getTables", notes = "get tables", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "system"),
        @ApiImplicitParam(name = "database", required = true, dataType = "String", value = "database")
    })
    @RequestMapping(value = "/getTables", method = RequestMethod.GET)
    public Message getTables(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("database") String database,
            @RequestParam("system") String system,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }
            List<String> tables =
                    metadataQueryService.getTablesByDsName(
                            dataSourceName,
                            database,
                            system,
                            SecurityFilter.getLoginUsername(request));
            return Message.ok().data("tables", tables);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get table list[获取表信息失败], name:["
                            + dataSourceName
                            + "]"
                            + ", system:["
                            + system
                            + "], database:["
                            + database
                            + "]",
                    e);
        }
    }

    @ApiOperation(value = "getTableProps", notes = "get table props", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "system"),
        @ApiImplicitParam(name = "database", required = true, dataType = "String", value = "database"),
        @ApiImplicitParam(name = "table", required = true, dataType = "String", value = "table")
    })
    @RequestMapping(value = "/getTableProps", method = RequestMethod.GET)
    public Message getTableProps(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("database") String database,
            @RequestParam("table") String table,
            @RequestParam("system") String system,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }
            Map<String, String> tableProps =
                    metadataQueryService.getTablePropsByDsName(
                            dataSourceName,
                            database,
                            table,
                            system,
                            SecurityFilter.getLoginUsername(request));
            return Message.ok().data("props", tableProps);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get table properties[获取表参数信息失败], name:["
                            + dataSourceName
                            + "]"
                            + ", system:["
                            + system
                            + "], database:["
                            + database
                            + "], table:["
                            + table
                            + "]",
                    e);
        }
    }

    @ApiOperation(value = "getPartitions", notes = "get partitions", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "system"),
        @ApiImplicitParam(name = "database", required = true, dataType = "String", value = "database"),
        @ApiImplicitParam(name = "table", required = true, dataType = "String", value = "table")
    })
    @RequestMapping(value = "/getPartitions", method = RequestMethod.GET)
    public Message getPartitions(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("database") String database,
            @RequestParam("table") String table,
            @RequestParam("system") String system,
            @RequestParam(name = "traverse", required = false, defaultValue = "false")
                    Boolean traverse,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }
            MetaPartitionInfo partitionInfo =
                    metadataQueryService.getPartitionsByDsName(
                            dataSourceName,
                            database,
                            table,
                            system,
                            traverse,
                            SecurityFilter.getLoginUsername(request));
            return Message.ok().data("partitions", partitionInfo);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get partitions[获取表分区信息失败], name:["
                            + dataSourceName
                            + "]"
                            + ", system:["
                            + system
                            + "], database:["
                            + database
                            + "], table:["
                            + table
                            + "]",
                    e);
        }
    }

    @ApiOperation(value = "getPartitionProps", notes = "get partition pProps", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "System"),
        @ApiImplicitParam(name = "database", required = true, dataType = "String", value = "database"),
        @ApiImplicitParam(name = "table", required = true, dataType = "String", value = "Table"),
        @ApiImplicitParam(name = "partition", required = true, dataType = "String", value = "partition")
    })
    @RequestMapping(value = "getPartitionProps", method = RequestMethod.GET)
    public Message getPartitionProps(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("database") String database,
            @RequestParam("table") String table,
            @RequestParam("partition") String partition,
            @RequestParam("system") String system,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }
            Map<String, String> partitionProps =
                    metadataQueryService.getPartitionPropsByDsName(
                            dataSourceName,
                            database,
                            table,
                            partition,
                            system,
                            SecurityFilter.getLoginUsername(request));
            return Message.ok().data("props", partitionProps);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get partition properties[获取分区参数信息失败], name:["
                            + dataSourceName
                            + "]"
                            + ", system:["
                            + system
                            + "], database:["
                            + database
                            + "], table:["
                            + table
                            + "], partition:["
                            + partition
                            + "]",
                    e);
        }
    }

    @ApiOperation(value = "getColumns", notes = "get columns", response = Message.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "dataSourceName", required = true, dataType = "String", value = "data source name"),
        @ApiImplicitParam(name = "system", required = true, dataType = "String", value = "system"),
        @ApiImplicitParam(name = "database", required = true, dataType = "String", value = "database"),
        @ApiImplicitParam(name = "table", required = true, dataType = "String", value = "table")
    })
    @RequestMapping(value = "/getColumns", method = RequestMethod.GET)
    public Message getColumns(
            @RequestParam("dataSourceName") String dataSourceName,
            @RequestParam("database") String database,
            @RequestParam("table") String table,
            @RequestParam("system") String system,
            HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(system)) {
                return Message.error("'system' is missing[缺少系统名]");
            }
            List<MetaColumnInfo> columns =
                    metadataQueryService.getColumnsByDsName(
                            dataSourceName,
                            database,
                            table,
                            system,
                            SecurityFilter.getLoginUsername(request));
            return Message.ok().data("columns", columns);
        } catch (Exception e) {
            return errorToResponseMessage(
                    "Fail to get column list[获取表字段信息失败], name:["
                            + dataSourceName
                            + "]"
                            + ", system:["
                            + system
                            + "], database:["
                            + database
                            + "], table:["
                            + table
                            + "]",
                    e);
        }
    }

    private Message errorToResponseMessage(String uiMessage, Exception e) {
        if (e instanceof MetaMethodInvokeException) {
            MetaMethodInvokeException invokeException = (MetaMethodInvokeException) e;
            if (logger.isDebugEnabled()) {
                String argumentJson = null;
                try {
                    argumentJson = Json.toJson(invokeException.getArgs(), null);
                } catch (Exception je) {
                    // Ignore
                }
                logger.trace(
                        uiMessage
                                + " => Method: "
                                + invokeException.getMethod()
                                + ", Arguments:"
                                + argumentJson,
                        e);
            }
            uiMessage +=
                    " possible reason[可能原因]: (" + invokeException.getCause().getMessage() + ")";
        } else {
            if (e instanceof ErrorException) {
                uiMessage += " possible reason[可能原因]: (" + e.getMessage() + ")";
            }
        }
        logger.error(uiMessage, e);
        return Message.error(uiMessage);
    }
}

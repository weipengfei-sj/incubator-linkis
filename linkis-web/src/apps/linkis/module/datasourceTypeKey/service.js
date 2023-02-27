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

import api from '@/common/service/api';

const dataSourceTypeBaseUrl = '/basedata-manager/datasource-type-key'

const getList = (params)=> {
  return api.fetch(dataSourceTypeBaseUrl, params , 'get')
}

const add = (data)=> {
  return api.fetch(dataSourceTypeBaseUrl, data , 'post')
}

const edit = (data)=> {
  return api.fetch(dataSourceTypeBaseUrl, data , 'put')
}

const del = (params)=> {
  return api.fetch(`${dataSourceTypeBaseUrl}/${params.id}`,'delete')
}

const getAllEnv = () => {
  return api.fetch(`/data-source-manager/type/all`, 'get')
}

export{
  getList,
  add,
  edit,
  del,
  getAllEnv
}

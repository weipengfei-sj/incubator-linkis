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
 
package org.apache.linkis.cs.common.serialize.impl.context;

import org.apache.linkis.cs.common.entity.source.CombinedNodeIDContextID;
import org.apache.linkis.cs.common.exception.CSErrorException;
import org.apache.linkis.cs.common.serialize.AbstractSerializer;
import org.apache.linkis.cs.common.utils.CSCommonUtils;


public class CombinedNodeIDContextIDSerializer extends AbstractSerializer<CombinedNodeIDContextID> {


    @Override
    public String getType() {
        return "CombinedNodeIDContextID";
    }




    @Override
    public CombinedNodeIDContextID fromJson(String json) throws CSErrorException {
        return CSCommonUtils.gson.fromJson(json, CombinedNodeIDContextID.class);
    }

    @Override
    public boolean accepts(Object obj) {

        if (null != obj && obj instanceof CombinedNodeIDContextID){
            return true;
        }
        return false;
    }


}


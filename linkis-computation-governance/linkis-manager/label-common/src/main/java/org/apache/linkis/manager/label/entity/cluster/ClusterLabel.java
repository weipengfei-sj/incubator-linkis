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
 
package org.apache.linkis.manager.label.entity.cluster;

import org.apache.linkis.manager.label.constant.LabelKeyConstant;
import org.apache.linkis.manager.label.entity.Feature;
import org.apache.linkis.manager.label.entity.GenericLabel;
import org.apache.linkis.manager.label.entity.annon.ValueSerialNum;

import java.util.HashMap;

public class ClusterLabel extends GenericLabel {

    public ClusterLabel() {
        setLabelKey(LabelKeyConstant.YARN_CLUSTER_KEY);
    }

    @Override
    public Feature getFeature() {
        return Feature.CORE;
    }

    @ValueSerialNum(1)
    public void setClusterName(String clusterName) {
        if (null == getValue()) {
            setValue(new HashMap<>());
        }
        getValue().put("clusterName", clusterName);
    }

    public String getClusterName() {
        if (null != getValue().get("clusterName")) {
            return getValue().get("clusterName");
        }
        return null;
    }

    @ValueSerialNum(0)
    public void setClusterType(String clusterType) {
        if (null == getValue()) {
            setValue(new HashMap<>());
        }
        getValue().put("clusterType", clusterType);
    }

    public String getClusterType(){
        if (null != getValue().get("clusterType")) {
            return getValue().get("clusterType");
        }
        return null;
    }


}

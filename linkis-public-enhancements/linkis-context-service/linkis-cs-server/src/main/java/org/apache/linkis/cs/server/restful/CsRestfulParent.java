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
 
package org.apache.linkis.cs.server.restful;

import org.apache.linkis.cs.common.entity.history.ContextHistory;
import org.apache.linkis.cs.common.entity.source.ContextID;
import org.apache.linkis.cs.common.entity.source.ContextKey;
import org.apache.linkis.cs.common.entity.source.ContextKeyValue;
import org.apache.linkis.cs.common.entity.source.ContextValue;
import org.apache.linkis.cs.common.exception.CSErrorException;
import org.apache.linkis.cs.server.enumeration.ServiceMethod;
import org.apache.linkis.cs.server.enumeration.ServiceType;
import org.apache.linkis.cs.server.protocol.HttpRequestProtocol;
import org.apache.linkis.cs.server.protocol.HttpResponseProtocol;
import org.apache.linkis.cs.server.protocol.RestResponseProtocol;
import org.apache.linkis.cs.server.scheduler.CsScheduler;
import org.apache.linkis.cs.server.scheduler.HttpAnswerJob;
import org.apache.linkis.cs.server.scheduler.RestJobBuilder;
import org.apache.linkis.cs.server.util.CsUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import com.fasterxml.jackson.databind.JsonNode;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface CsRestfulParent {

    default HttpAnswerJob submitRestJob(HttpServletRequest req,
                                        ServiceMethod method,
                                        Object... objects) throws InterruptedException {
        // TODO: 2020/3/3 单例
        HttpAnswerJob job = (HttpAnswerJob) new RestJobBuilder().build(getServiceType());
        HttpRequestProtocol protocol = job.getRequestProtocol();
        protocol.setUsername(SecurityFilter.getLoginUsername(req));
        protocol.setServiceMethod(method);
        protocol.setRequestObjects(objects);
        getScheduler().submit(job);
        return job;
    }

    default Message generateResponse(HttpAnswerJob job, String responseKey) throws CSErrorException {
        HttpResponseProtocol responseProtocol = job.getResponseProtocol();
        if (responseProtocol instanceof RestResponseProtocol) {
            Message message = ((RestResponseProtocol) responseProtocol).get();
            if (message == null) {
                return Message.error("job execute timeout");
            }
            int status = ((RestResponseProtocol) responseProtocol).get().getStatus();
            if (status == 1) {
                //failed
                return ((RestResponseProtocol) responseProtocol).get();
            } else if (status == 0) {
                Object data = job.getResponseProtocol().getResponseData();
                if (data == null) {
                    return Message.ok().data(responseKey, null);
                } else if (data instanceof List && ((List) data).isEmpty()) {
                    return Message.ok().data(responseKey, new String[]{});
                } else if (data instanceof List) {
                    ArrayList<String> strings = new ArrayList<>();
                    for (Object d : (List) data) {
                        strings.add(CsUtils.serialize(d));
                    }
                    return Message.ok().data(responseKey, strings);
                } else {
                    String dataStr = CsUtils.serialize(data);
                    return Message.ok().data(responseKey, dataStr);
                }
            } else {

            }
        }
        return Message.ok();
    }

    ServiceType getServiceType();

    CsScheduler getScheduler();

    default ContextID getContextIDFromJsonNode(JsonNode jsonNode) throws CSErrorException, IOException, ClassNotFoundException {
        return deserialize(jsonNode, "contextID");
    }

    default <T> T deserialize(JsonNode jsonNode, String key) throws CSErrorException {
        String str = jsonNode.get(key).textValue();
        return (T) CsUtils.SERIALIZE.deserialize(str);
    }

    default ContextKey getContextKeyFromJsonNode(JsonNode jsonNode) throws CSErrorException, IOException, ClassNotFoundException {
        return deserialize(jsonNode, "contextKey");
    }

    default ContextValue getContextValueFromJsonNode(JsonNode jsonNode) throws CSErrorException, IOException, ClassNotFoundException {
        return deserialize(jsonNode, "contextValue");
    }

    default ContextKeyValue getContextKeyValueFromJsonNode(JsonNode jsonNode) throws CSErrorException, IOException, ClassNotFoundException {
        return deserialize(jsonNode, "contextKeyValue");
    }

    default ContextHistory getContextHistoryFromJsonNode(JsonNode jsonNode) throws CSErrorException, IOException, ClassNotFoundException {
        return deserialize(jsonNode, "contextHistory");
    }

}

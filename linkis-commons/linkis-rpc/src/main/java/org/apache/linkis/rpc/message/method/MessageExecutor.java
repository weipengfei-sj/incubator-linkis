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

package org.apache.linkis.rpc.message.method;

import org.apache.linkis.common.utils.JavaLog;
import org.apache.linkis.protocol.message.RequestProtocol;
import org.apache.linkis.rpc.Sender;
import org.apache.linkis.rpc.message.exception.MessageErrorException;
import org.apache.linkis.rpc.message.exception.MessageWarnException;
import org.apache.linkis.rpc.message.utils.MessageUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageExecutor extends JavaLog {

    private List<MethodExecuteWrapper> getMinOrderMethodWrapper(
            Map<String, List<MethodExecuteWrapper>> methodWrappers) {
        // get min key order
        List<MethodExecuteWrapper> minOrderMethodWrapper = new ArrayList<>();
        methodWrappers.forEach(
                (k, v) ->
                        v.forEach(
                                m -> {
                                    if (MessageUtils.orderIsMin(m, v)) {
                                        minOrderMethodWrapper.add(m);
                                    }
                                }));
        return minOrderMethodWrapper;
    }

    public Object execute(
            RequestProtocol requestProtocol,
            Map<String, List<MethodExecuteWrapper>> methodWrappers,
            Sender sender)
            throws InterruptedException, MessageErrorException {
        Integer count = methodWrappers.values().stream().map(List::size).reduce(0, Integer::sum);
        if (count == 1) {
            return executeOneMethod(requestProtocol, methodWrappers, sender);
        } else {
            throw new MessageErrorException(
                    120001,
                    String.format(
                            "find %s method for the rpc request:%s",
                            count, requestProtocol.getClass().getName()));
        }
    }

    private Object executeOneMethod(
            RequestProtocol requestProtocol,
            Map<String, List<MethodExecuteWrapper>> methodWrappers,
            Sender sender) {
        List<MethodExecuteWrapper> methodExecuteWrappers = getMinOrderMethodWrapper(methodWrappers);
        Object result = null;
        if (methodExecuteWrappers.size() == 1) {
            MethodExecuteWrapper methodWrapper = methodExecuteWrappers.get(0);
            try {
                if (!methodWrapper.shouldSkip) {
                    Method method = methodWrapper.getMethod();
                    Object service = methodWrapper.getService();
                    if (methodWrapper.isHasSender()) {
                        if (methodWrapper.isSenderOnLeft()) {
                            result = method.invoke(service, sender, requestProtocol);
                        } else {
                            result = method.invoke(service, requestProtocol, sender);
                        }
                    } else {
                        result = method.invoke(service, requestProtocol);
                    }
                }
            } catch (Throwable t) {
                logger().error(String.format("method %s call failed", methodWrapper.getAlias()), t);
                throw new MessageWarnException(10000, "method call failed", t);
            }
        }
        return result;
    }
}

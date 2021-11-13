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
 
package org.apache.linkis.errorcode.client.utils;

import org.apache.linkis.errorcode.client.handler.LinkisErrorCodeHandler;
import org.apache.linkis.errorcode.common.ErrorCode;

import java.io.IOException;
import java.util.List;


public class Test {
    public static void main(String[] args) throws IOException {
        try{
            Class.forName("org.apache.linkis.errorcode.client.handler.LinkisErrorCodeHandler");
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LinkisErrorCodeHandler linkisErrorCodeHandler = LinkisErrorCodeHandler.getInstance();
        List<ErrorCode> ret = linkisErrorCodeHandler.handle("queue root is not exists in YARN \n queue root is not exists in YARN");
        System.out.println(ret);
        linkisErrorCodeHandler.close();
    }
}

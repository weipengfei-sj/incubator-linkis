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

package org.apache.linkis.rpc.transform

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.protocol.message.RequestProtocol
import org.apache.linkis.rpc.errorcode.LinkisRpcErrorCodeSummary
import org.apache.linkis.rpc.errorcode.LinkisRpcErrorCodeSummary.TRANSMITTED_BEAN_IS_NULL
import org.apache.linkis.rpc.exception.DWCURIException
import org.apache.linkis.rpc.serializer.ProtostuffSerializeUtil
import org.apache.linkis.server.{EXCEPTION_MSG, Message}

private[linkis] trait RPCProduct {

  def toMessage(t: Any): Message

  def notFound(): Message

  def ok(): Message

}

private[linkis] object RPCProduct extends Logging {

  private[rpc] val IS_REQUEST_PROTOCOL_CLASS = "rpc_is_request_protocol"
  private[rpc] val CLASS_VALUE = "rpc_object_class"
  private[rpc] val OBJECT_VALUE = "rpc_object_value"

  private val rpcProduct: RPCProduct = new RPCProduct {

    override def toMessage(t: Any): Message = {
      if (t == null) {
        throw new DWCURIException(
          TRANSMITTED_BEAN_IS_NULL.getErrorCode,
          TRANSMITTED_BEAN_IS_NULL.getErrorDesc
        )
      }
      val message = Message.ok("RPC Message.")
      if (isRequestProtocol(t)) {
        message.data(IS_REQUEST_PROTOCOL_CLASS, "true")
      } else {
        message.data(IS_REQUEST_PROTOCOL_CLASS, "false")
      }
      message.data(OBJECT_VALUE, ProtostuffSerializeUtil.serialize(t))
      message.setMethod("/rpc/message")
      message.data(CLASS_VALUE, t.getClass.getName)
    }

    override def notFound(): Message = {
      val message = Message.error("RPC Message.")
      message.setMethod("/rpc/message")
      message.data(
        EXCEPTION_MSG,
        new DWCURIException(
          LinkisRpcErrorCodeSummary.URL_ERROR.getErrorCode,
          LinkisRpcErrorCodeSummary.URL_ERROR.getErrorDesc
        ).toMap
      )
    }

    override def ok(): Message = {
      val message = Message.ok("RPC Message.")
      message.setMethod("/rpc/message")
      message
    }

  }

  private[rpc] def isRequestProtocol(obj: Any): Boolean = obj.isInstanceOf[RequestProtocol]

  def getRPCProduct: RPCProduct = rpcProduct
}

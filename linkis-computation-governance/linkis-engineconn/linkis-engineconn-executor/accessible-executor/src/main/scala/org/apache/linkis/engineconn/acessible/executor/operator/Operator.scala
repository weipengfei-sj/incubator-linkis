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
 
package org.apache.linkis.engineconn.acessible.executor.operator

import org.apache.linkis.engineconn.common.exception.EngineConnException

trait Operator {

  def getNames: Array[String]

  def apply(implicit parameters: Map[String, Any]): Map[String, Any]

  protected def getAs[T](key: String, defaultVal: T)(implicit parameters: Map[String, Any]): T =
    parameters.getOrElse(key, defaultVal) match {
      case t: T => t
      case null => null.asInstanceOf[T]
      case v => throw EngineConnException(20305, s"Unknown $v for key $key.")
    }

}
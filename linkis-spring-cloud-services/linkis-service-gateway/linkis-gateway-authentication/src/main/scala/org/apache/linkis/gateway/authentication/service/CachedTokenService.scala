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

package org.apache.linkis.gateway.authentication.service

import java.util.concurrent.{ExecutionException, TimeUnit}
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import org.apache.linkis.common.exception.ErrorException
import org.apache.linkis.common.utils.Utils
import org.apache.linkis.gateway.authentication.bo.Token
import org.apache.linkis.gateway.authentication.exception.TokenNotExistException
import org.apache.linkis.gateway.authentication.bo.impl.TokenImpl
import org.apache.linkis.gateway.authentication.bo.{Token, User}
import org.apache.linkis.gateway.authentication.conf.TokenConfiguration
import org.apache.linkis.gateway.authentication.dao.TokenDao
import org.apache.linkis.gateway.authentication.entity.TokenEntity
import org.apache.linkis.gateway.authentication.exception.{TokenAuthException, TokenNotExistException}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CachedTokenService extends TokenService {

  @Autowired
  private var tokenDao: TokenDao = _

  private val tokenCache: LoadingCache[String, Token] = CacheBuilder.newBuilder
    .maximumSize(TokenConfiguration.TOKEN_CACHE_MAX_SIZE)
    .refreshAfterWrite(TokenConfiguration.TOKEN_CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
    .build(
      new CacheLoader[String, Token]() {
        @Override
        def load(tokenName: String): Token = {
          val tokenEntityList: java.util.List[TokenEntity] = tokenDao.selectTokenByName(tokenName)
          if (tokenEntityList != null && tokenEntityList.size != 0) {
            new TokenImpl().convertFrom(tokenEntityList.get(0))
          } else {
            throw new TokenNotExistException(15204, s"Invalid Token")
          }
        }
      }
    );

//  def setTokenDao(tokenDao: TokenDao): Unit = {
//    this.tokenDao = tokenDao
//  }

  /*
    TODO begin
   */
  override def addNewToken(token: Token): Boolean = {
    false
  }

  override def removeToken(tokenName: String): Boolean = {
    false
  }

  override def updateToken(token: Token): Boolean = {
    false
  }

  override def addUserForToken(tokenName: String, user: User): Boolean = {
    false
  }

  override def addHostForToken(tokenName: String, ip: String): Boolean = {
    false
  }

  override def addHostAndUserForToken(tokenName: String, user: User, ip: String): Boolean = {
    false
  }

  override def removeUserForToken(tokenName: String, user: User): Boolean = {
    false
  }

  override def removeHostForToken(tokenName: String, ip: String): Boolean = {
    false
  }

  /*
    TODO end
   */

  private def loadTokenFromCache(tokenName: String): Token = {
    if (tokenName == null) {
      throw new TokenAuthException(15205, "Token is null!")
    }
    Utils.tryCatch(tokenCache.get(tokenName))(
      t => t match {
        case x: ExecutionException => x.getCause match {
          case _: TokenNotExistException => null
          case _ => throw new TokenAuthException(15200, "Failed to load token from DB into cache!")
        }
        case _ => throw new TokenAuthException(15200, "Failed to load token from DB into cache!")
      }
    )
  }

  private def isTokenAcceptableWithUser(token: Token, userName: String): Boolean = {
    token != null && !token.isStale() && token.isUserLegal(userName)
  }

  private def isTokenValid(token: Token): Boolean = {
    token != null && !token.isStale()
  }

  private def isTokenAcceptableWithHost(token: Token, host: String): Boolean = {
    token != null && !token.isStale() && token.isHostLegal(host)
  }

  override def isTokenValid(tokenName: String): Boolean = {
    isTokenValid(loadTokenFromCache(tokenName))
  }

  override def isTokenAcceptableWithUser(tokenName: String, userName: String): Boolean = {
    isTokenAcceptableWithUser(loadTokenFromCache(tokenName), userName)
  }

  override def isTokenAcceptableWithHost(tokenName: String, host: String): Boolean = {
    isTokenAcceptableWithHost(loadTokenFromCache(tokenName), host)
  }

  override def doAuth(tokenName: String, userName: String, host: String): Boolean = {
    val tmpToken: Token = loadTokenFromCache(tokenName)
    var ok: Boolean = true
    if (!isTokenValid(tmpToken)) {
      ok = false
      throw new TokenAuthException(15201, "Token is not valid or stale!")
    }
    if (!isTokenAcceptableWithUser(tmpToken, userName)) {
      ok = false
      throw new TokenAuthException(15202, "Illegal TokenUser for Token!")
    }
    if (!isTokenAcceptableWithHost(tmpToken, host)) {
      ok = false
      throw new TokenAuthException(15203, "Illegal Host for Token!")
    }
    ok
  }
}

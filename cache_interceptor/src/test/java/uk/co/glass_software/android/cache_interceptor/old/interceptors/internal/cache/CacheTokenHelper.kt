/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.old.interceptors.internal.cache

import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import java.util.*

//object CacheTokenHelper {
//
//    fun verifyCacheToken(cacheToken: CacheToken,
//                         expectedUrl: String,
//                         expectedBody: String,
//                         expectedKey: String,
//                         cacheDate: Date,
//                         expiryDate: Date,
//                         fetchDate: Date,
//                         expectedObservable: Observable<*>,
//                         expectedResponseClass: Class<*>,
//                         expectedStatus: CacheStatus,
//                         expectedTtl: Float) {
//        assertEquals("CacheToken URL didn't match", expectedUrl, cacheToken.apiUrl)
//        assertEquals("CacheToken uniqueParameters didn't match", expectedBody, cacheToken.uniqueParameters)
//        assertEquals("CacheToken key didn't match", expectedKey, cacheToken.getKey(Hasher(null)))
//        assertEquals("CacheToken cached date didn't match", cacheDate, cacheToken.cacheDate)
//        assertEquals("CacheToken expiry date didn't match", expiryDate, cacheToken.expiryDate)
//        assertEquals("CacheToken fetch date didn't match", fetchDate, cacheToken.fetchDate)
//        assertEquals("CacheToken refresh observable didn't match", expectedObservable, cacheToken.getRefreshObservable())
//        assertEquals("CacheToken response class didn't match", expectedResponseClass, cacheToken.getResponseClass())
//        assertEquals("CacheToken status didn't match", expectedStatus, cacheToken.status)
//        assertEquals("CacheToken TTL didn't match", expectedTtl, cacheToken.getTtlInMinutes())
//    }
//}

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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache;

import java.util.Date;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;

import static junit.framework.Assert.assertEquals;

public class CacheTokenHelper {
    
    public static void verifyCacheToken(CacheToken cacheToken,
                                        String expectedUrl,
                                        String expectedBody,
                                        String expectedKey,
                                        Date cacheDate,
                                        Date expiryDate,
                                        Date fetchDate,
                                        Observable expectedObservable,
                                        Class expectedResponseClass,
                                        CacheStatus expectedStatus,
                                        float expectedTtl) {
        assertEquals("CacheToken URL didn't match", expectedUrl, cacheToken.getApiUrl());
        assertEquals("CacheToken uniqueParameters didn't match", expectedBody, cacheToken.getUniqueParameters());
        assertEquals("CacheToken key didn't match", expectedKey, cacheToken.getKey(new Hasher(null)));
        assertEquals("CacheToken cached date didn't match", cacheDate, cacheToken.getCacheDate());
        assertEquals("CacheToken expiry date didn't match", expiryDate, cacheToken.getExpiryDate());
        assertEquals("CacheToken fetch date didn't match", fetchDate, cacheToken.getFetchDate());
        assertEquals("CacheToken refresh observable didn't match", expectedObservable, cacheToken.getRefreshObservable());
        assertEquals("CacheToken response class didn't match", expectedResponseClass, cacheToken.getResponseClass());
        assertEquals("CacheToken status didn't match", expectedStatus, cacheToken.getStatus());
        assertEquals("CacheToken TTL didn't match", expectedTtl, cacheToken.getTtlInMinutes());
    }
}

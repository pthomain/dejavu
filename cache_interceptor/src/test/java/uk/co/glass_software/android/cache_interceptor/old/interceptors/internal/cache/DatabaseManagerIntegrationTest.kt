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

//class DatabaseManagerIntegrationTest : BaseIntegrationTest() {
//
//    private var stubbedResponse: TestResponse? = null
//    private var cacheToken: CacheToken<TestResponse>? = null
//    private var mockUpstream: Observable<TestResponse>? = null
//
//    private var target: DatabaseManager<*>? = null
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        val requestToken = CacheToken.newRequest(
//                TestResponse::class.java,
//                "apiUrl",
//                "",
//                5
//        )
//
//        stubbedResponse = assetHelper.getStubbedResponse<ApiError, TestResponse>(TestResponse.STUB_FILE, TestResponse::class.java).blockingFirst()
//        stubbedResponse!!.metadata = ResponseMetadata.create(requestToken, null)
//
//        mockUpstream = mock<Observable<*>>(Observable<*>::class.java)
//
//        val fetchDate = Date()
//        val expiryDate = Date(fetchDate.time + 12345)
//
//        cacheToken = CacheToken.caching(requestToken,
//                mockUpstream,
//                fetchDate,
//                fetchDate,
//                expiryDate
//        )
//
//        stubbedResponse!!.metadata.setCacheToken(cacheToken)
//
//        target = dependencyHelper.getDatabaseManager()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testCacheAndFlush() {
//        assertNull("Cache should not contain anything", target!!.getCachedResponse(mockUpstream!!, cacheToken!!.toLong()))
//
//        target!!.cache(stubbedResponse)
//
//        val cachedResponse = target!!.getCachedResponse(mockUpstream!!, cacheToken!!.toLong())
//        cachedResponse!!.metadata = stubbedResponse!!.metadata //ignore metadata, covered by unit test
//        assertEquals("Cached response didn't match the original", stubbedResponse, cachedResponse)
//
//        target!!.clearCache()
//
//        assertNull("Cache should not contain anything", target!!.getCachedResponse(mockUpstream!!, cacheToken!!.toLong()))
//    }
//
//    companion object {
//
//        //used by other tests to preserve encapsulation
//        @VisibleForTesting
//        fun <E : Exception, R : ResponseMetadata.Holder<R, E>> cache(databaseManager: DatabaseManager<*>,
//                                                                     response: R,
//                                                                     cacheToken: CacheToken) where E : Function<E, Boolean>, {
//            response.setMetadata(ResponseMetadata.create(cacheToken, null))
//            databaseManager.cache(response)
//        }
//
//        //used by other tests to preserve encapsulation
//        @VisibleForTesting
//        fun <E : Exception, R : ResponseMetadata.Holder<R, E>> getCachedResponse(databaseManager: DatabaseManager<*>,
//                                                                                 upstream: Observable<R>,
//                                                                                 cacheToken: CacheToken): R? where E : Function<E, Boolean>, {
//            return databaseManager.getCachedResponse(upstream, cacheToken.toLong())
//        }
//
//        //used by other tests to preserve encapsulation
//        @VisibleForTesting
//        fun <E : Exception, R : ResponseMetadata.Holder<R, E>> getCachedCacheToken(cacheToken: CacheToken<R>,
//                                                                                   refreshObservable: Observable<R>,
//                                                                                   cacheDate: Date,
//                                                                                   expiryDate: Date): CacheToken<R> where E : Function<E, Boolean>, {
//            return CacheToken.cached(cacheToken,
//                    refreshObservable,
//                    cacheDate,
//                    expiryDate
//            )
//        }
//
//        //used by other tests to preserve encapsulation
//        @VisibleForTesting
//        fun prepareSpyCacheManagerToReturnStale(spyCacheManager: CacheManager<*>) {
//            doReturn(STALE).`when`<CacheManager>(spyCacheManager).getCachedStatus(any<T>())
//        }
//    }
//}
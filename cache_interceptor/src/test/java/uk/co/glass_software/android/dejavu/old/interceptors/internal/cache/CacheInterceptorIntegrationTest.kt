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

package uk.co.glass_software.android.dejavu.old.interceptors.internal.cache


//class CacheInterceptorIntegrationTest : BaseIntegrationTest() {
//
//    private val apiUrl = "http://test.com"
//    private val body = "someBody"
//    private var databaseManager: DatabaseManager<*>? = null
//    private var cachedResponse: TestResponse? = null
//    private var networkResponse: TestResponse? = null
//    private var spyCacheManager: CacheManager<*>? = null
//
//    private val response: TestResponse
//        get() = assetHelper.getStubbedResponse(TestResponse.STUB_FILE, TestResponse::class.java)
//                .doOnNext { response ->
//                    response.metadata = ResponseMetadata.create(getToken(false), null
//                    )
//                }
//                .blockingFirst()
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        databaseManager = dependencyHelper.getDatabaseManager()
//        cachedResponse = response
//        networkResponse = response
//        networkResponse!!.remove(0)//removing element to test an updated response
//
//        spyCacheManager = spy(dependencyHelper.getCacheManager())
//    }
//
//    private fun getInterceptor(provideCacheManager: CacheManager<*>?,
//                               isCacheEnabled: Boolean,
//                               isDoNotCache: Boolean,
//                               isNetworkError: Function<Glitch, Boolean>): CacheInterceptor<Glitch, TestResponse> {
//        return CacheInterceptor<E>(
//                provideCacheManager!!,
//                isCacheEnabled,
//                mock(Logger::class.java),
//                isNetworkError,
//                getToken(isDoNotCache).toLong()
//        )
//    }
//
//    private fun getToken(isDoNotCache: Boolean): CacheToken<TestResponse> {
//        return if (isDoNotCache)
//            CacheToken.doNotCache(TestResponse::class.java)
//        else
//            CacheToken.newRequest(TestResponse::class.java, apiUrl, body, 5)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorNoCaching() {
//        testInterceptor(false,
//                false,
//                false,
//                false,
//                NOT_CACHED,
//                false
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorDoNotCacheToken() {
//        testInterceptor(true,
//                true,
//                false,
//                false,
//                NOT_CACHED,
//                false
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorWithFreshCachedResponse() {
//        testInterceptor(true,
//                false,
//                true,
//                false,
//                CACHED,
//                false
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorWithStaleCachedResponseNetworkError() {
//        testInterceptor(true,
//                false,
//                true,
//                true,
//                COULD_NOT_REFRESH,
//                true
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorWithStaleCachedResponseNoNetworkError() {
//        testInterceptor(true,
//                false,
//                true,
//                true,
//                REFRESHED,
//                false
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorNoCachedResponseNetworkError() {
//        testInterceptor(true,
//                false,
//                false,
//                false,
//                NOT_CACHED,
//                true
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testInterceptorNoCachedResponseNoNetworkError() {
//        testInterceptor(true,
//                false,
//                false,
//                false,
//                FRESH,
//                false
//        )
//    }
//
//    private fun testInterceptor(isCachingEnabled: Boolean,
//                                isDoNotCache: Boolean,
//                                hasCachedResponse: Boolean,
//                                isCachedResponseStale: Boolean,
//                                expectedStatus: CacheStatus,
//                                isNetworkError: Boolean) {
//        val existingCacheToken: CacheToken<TestResponse>
//
//
//        if (!isCachingEnabled) { //this overrides default tokens to test the "isCachingEnabled" config only
//            existingCacheToken = getToken(false)
//        } else if (hasCachedResponse) {
//            existingCacheToken = CacheToken.cached(getToken(false), null,
//                    Date(),
//                    Date(System.currentTimeMillis() + 5000 * 60)
//            )
//        } else if (isNetworkError || isDoNotCache) {
//            existingCacheToken = getToken(true)
//        } else {
//            existingCacheToken = getToken(false)
//        }
//
//        if (hasCachedResponse) {
//            DatabaseManagerIntegrationTest.cache(databaseManager!!, cachedResponse!!, existingCacheToken)
//
//            assertNotNull("Response should have been cached",
//                    DatabaseManagerIntegrationTest.getCachedResponse<Exception, ResponseMetadata.Holder<R, E>>(databaseManager!!, null, existingCacheToken)
//            )
//        } else {
//            assertNull("Response should not be cached", DatabaseManagerIntegrationTest.getCachedResponse<Exception, ResponseMetadata.Holder<R, E>>(databaseManager!!, null, existingCacheToken))
//        }
//
//        val errorResponse = TestResponse()
//        errorResponse.metadata = ResponseMetadata.create(
//                getToken(true),
//                Glitch(IOException("test"), 404, ErrorCode.NOT_FOUND, "some Exception")
//        )
//
//        val expectedResponse: TestResponse?
//        if (expectedStatus === FRESH || expectedStatus === REFRESHED) {
//            expectedResponse = networkResponse
//        } else if (expectedStatus === NOT_CACHED) {
//            if (isNetworkError) {
//                expectedResponse = errorResponse
//            } else {
//                expectedResponse = networkResponse
//            }
//        } else if (hasCachedResponse && (expectedStatus === CACHED || expectedStatus === STALE || expectedStatus === COULD_NOT_REFRESH)) {
//            expectedResponse = cachedResponse
//        } else {
//            expectedResponse = errorResponse
//        }
//
//        if (isCachedResponseStale) {
//            DatabaseManagerIntegrationTest.prepareSpyCacheManagerToReturnStale(spyCacheManager)
//        }
//
//        val interceptor = getInterceptor(spyCacheManager,
//                isCachingEnabled,
//                isDoNotCache,
//                { ignore -> isNetworkError }
//        )
//
//        val observable = Observable.just<TestResponse>(if (isNetworkError) errorResponse else networkResponse)
//
//        if (isCachedResponseStale) {
//            val testResponses = observable.compose(interceptor).toList().blockingGet()
//
//            assertResponse(testResponses.get(0), cachedResponse, STALE)
//            assertResponse(testResponses.get(1),
//                    if (isNetworkError) cachedResponse else networkResponse,
//                    if (isNetworkError) COULD_NOT_REFRESH else REFRESHED
//            )
//        } else {
//            assertResponse(observable.compose(interceptor).blockingFirst(), expectedResponse, expectedStatus)
//        }
//    }
//
//    private fun assertResponse(testResponse: TestResponse,
//                               expectedResponse: TestResponse,
//                               expectedStatus: CacheStatus) {
//        assertEquals("Responses didn't match", expectedResponse, testResponse)
//
//        val metadata = expectedResponse.metadata
//        if (metadata.hasError()) {
//            assertNotNull("Response should have an error", metadata.getError())
//            assertEquals("Errors did not match", metadata.getError(), testResponse.metadata.getError())
//        } else {
//            assertNull("Response should have no error", testResponse.metadata.getError())
//        }
//
//        val cacheToken = testResponse.metadata.cacheToken
//
//        assertEquals("Response class didn't match", TestResponse::class.java, cacheToken.getResponseClass())
//
//        if (cacheToken.status !== NOT_CACHED && cacheToken.status !== CacheInstruction.Operation.Type.DO_NOT_CACHE) {
//            assertEquals("Cache token url didn't match", apiUrl, cacheToken.apiUrl)
//            assertEquals("Body didn't match", body, cacheToken.uniqueParameters)
//            assertNotNull("Fetch date should not be null", cacheToken.fetchDate)
//            assertNotNull("Cache date should not be null", cacheToken.cacheDate)
//            assertNotNull("Expiry date should not be null", cacheToken.expiryDate)
//        } else {
//            assertNotNull("Fetch date should not be null", cacheToken.fetchDate)
//            assertNull("Cache date should not be null", cacheToken.cacheDate)
//            assertNull("Expiry date should not be null", cacheToken.expiryDate)
//        }
//
//        assertEquals("Cache token should be $expectedStatus", expectedStatus, cacheToken.status)
//    }
//}
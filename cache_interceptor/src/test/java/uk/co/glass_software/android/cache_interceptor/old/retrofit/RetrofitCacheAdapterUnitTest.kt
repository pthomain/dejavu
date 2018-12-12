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

package uk.co.glass_software.android.cache_interceptor.old.retrofit

//class RetrofitCacheAdapterUnitTest {
//
//    private var mockInterceptorFactory: RxCacheInterceptor.Factory<ApiError>? = null
//    private var mockCallAdapter: CallAdapter<*, *>? = null
//
//    private var target: RetrofitCacheAdapter<ApiError>? = null
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        mockInterceptorFactory = mock<RxCacheInterceptor.Factory<*>>(RxCacheInterceptor.Factory<*>::class.java)
//        mockCallAdapter = mock(CallAdapter<*, *>::class.java)
//
//        target = RetrofitCacheAdapter(
//                mockInterceptorFactory,
//                TestResponse::class.java,
//                mockCallAdapter
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testAdapt() {
//        val mockCall = mock(Call<*>::class.java)
//        val url = HttpUrl.parse("http://test.com")
//        val mockBody = RequestBody.create(MediaType.parse("text/plain"), "uniqueParameters")
//        val mockRequest = Request.Builder().url(url!!).put(mockBody).build()
//        val mockCacheInterceptor = mock(RxCacheInterceptor<*>::class.java)
//        val mockUpstreamResponse = mock(TestResponse::class.java)
//        val mockDownstreamResponse = mock(TestResponse::class.java)
//        val mockUpstreamObservable = Observable.just(mockUpstreamResponse)
//        val mockDownstreamObservable = Observable.just(mockDownstreamResponse)
//
//        `when`(mockCallAdapter!!.adapt(eq<Call>(mockCall))).thenReturn(mockUpstreamObservable)
//        `when`(mockCall.request()).thenReturn(mockRequest)
//        `when`<Observable>(mockCacheInterceptor.apply(eq(mockUpstreamObservable))).thenReturn(mockDownstreamObservable)
//
//        doReturn(mockCacheInterceptor).`when`(mockInterceptorFactory).create(
//                eq(TestResponse::class.java),
//                eq(url.toString()),
//                eq(mockBody.toString())
//        )
//
//        assertEquals(mockDownstreamResponse, (target!!.adapt(mockCall) as Observable<TestResponse>).blockingFirst())
//    }
//}
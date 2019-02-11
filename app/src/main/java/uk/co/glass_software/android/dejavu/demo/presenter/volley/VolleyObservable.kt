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

package uk.co.glass_software.android.dejavu.demo.presenter.volley

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Observer
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.DejaVuTransformer
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch

class VolleyObservable<E, R> private constructor(private val requestQueue: RequestQueue,
                                                 private val gson: Gson,
                                                 private val responseClass: Class<R>,
                                                 private val url: String)
    : Observable<R>()
        where E : Exception,
              E : NetworkErrorProvider {

    private lateinit var observer: Observer<in R>

    override fun subscribeActual(observer: Observer<in R>) {
        this.observer = observer
        requestQueue.add(StringRequest(
                Request.Method.GET,
                url.toString(),
                Response.Listener(this::onResponse),
                Response.ErrorListener(this::onError)
        ))
    }

    private fun onResponse(response: String) {
        observer.onNext(gson.fromJson(response, responseClass))
        observer.onComplete()
    }

    private fun onError(volleyError: VolleyError) {
        observer.onError(volleyError)
    }

    companion object {

        fun <E, R> create(requestQueue: RequestQueue,
                          gson: Gson,
                          responseClass: Class<R>,
                          cacheInterceptor: DejaVuTransformer,
                          url: String)
                where E : Exception,
                      E : NetworkErrorProvider =
                VolleyObservable<E, R>(
                        requestQueue,
                        gson,
                        responseClass,
                        url
                ).cast(Any::class.java)
                        .compose(cacheInterceptor)
                        .cast(responseClass)!!

        fun <R> createDefault(requestQueue: RequestQueue,
                              gson: Gson,
                              responseClass: Class<R>,
                              cacheInterceptor: DejaVuTransformer,
                              url: String) =
                create<Glitch, R>(
                        requestQueue,
                        gson,
                        responseClass,
                        cacheInterceptor,
                        url
                )
    }
}

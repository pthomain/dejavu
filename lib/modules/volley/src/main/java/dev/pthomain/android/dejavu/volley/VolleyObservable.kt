package dev.pthomain.android.dejavu.volley

/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.Observer

class VolleyObservable<E, R : Any> private constructor(
        private val requestQueue: RequestQueue,
        private val serialiser: Serialiser,
        private val requestMetadata: RequestMetadata<R>
) : Observable<R>()
        where E : Throwable,
              E : NetworkErrorPredicate {

    private lateinit var observer: Observer<in R>

    override fun subscribeActual(observer: Observer<in R>) {
        this.observer = observer
        requestQueue.add(StringRequest(
                Request.Method.GET,
                requestMetadata.url,
                Listener(::onResponse),
                ErrorListener(::onError)
        ))
    }

    private fun onResponse(response: String) {
        observer.onNext(serialiser.deserialise(response, requestMetadata.responseClass))
        observer.onComplete()
    }

    private fun onError(volleyError: VolleyError) {
        observer.onError(volleyError)
    }

    class Factory<E>(
            private val serialiser: Serialiser,
            private val dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <R : Any> create(
                requestQueue: RequestQueue,
                isWrapped: Boolean,
                operation: Operation,
                requestMetadata: PlainRequestMetadata<R>
        ): Observable<R> =
                VolleyObservable<E, R>(
                        requestQueue,
                        serialiser,
                        requestMetadata
                ).compose(
                        dejaVuInterceptorFactory.create(
                                isWrapped,
                                operation,
                                requestMetadata
                        )
                ).cast(requestMetadata.responseClass)!!

    }
}

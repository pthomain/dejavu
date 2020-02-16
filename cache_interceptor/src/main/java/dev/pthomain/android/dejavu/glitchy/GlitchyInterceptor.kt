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

package dev.pthomain.android.dejavu.glitchy

import dev.pthomain.android.glitchy.interceptor.Interceptor
import dev.pthomain.android.glitchy.interceptor.Interceptor.SimpleInterceptor
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import io.reactivex.Observable
import io.reactivex.ObservableSource

internal class GlitchyInterceptor<E>(errorFactory: ErrorFactory<E>) : SimpleInterceptor()
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun apply(upstream: Observable<Any>): ObservableSource<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Factory<E>(private val errorFactory: ErrorFactory<E>) : Interceptor.Factory<E>
            where E : Throwable,
                  E : NetworkErrorPredicate {

        override fun <M> create(parsedType: ParsedType<M>) =
                GlitchyInterceptor(errorFactory)
    }

}
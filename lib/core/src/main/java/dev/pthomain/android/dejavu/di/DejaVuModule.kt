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

package dev.pthomain.android.dejavu.di

import android.content.Context
import android.net.Uri
import dagger.Module
import dagger.Provides
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.shared.utils.Function1
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import javax.inject.Singleton

@Module(includes = [SharedModule::class])
abstract class DejaVuModule<E>(
        private val context: Context,
        private val errorFactory: ErrorFactory<E>,
        private val operationPredicate: (RequestMetadata<*>) -> Operation.Remote?,
        private val durationPredicate: (TransientResponse<*>) -> Int?
) where E : Throwable,
        E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideContext() = context

    @Provides
    @Singleton
    fun provideErrorFactory() = errorFactory

    @Provides
    @Singleton
    fun provideOperationPredicate() =
            object : Function1<RequestMetadata<*>, Operation.Remote?> {
                override fun get(t1: RequestMetadata<*>) = operationPredicate(t1)
            }

    @Provides
    @Singleton
    fun provideDurationPredicate() =
            object : Function1<TransientResponse<*>, Int?> {
                override fun get(t1: TransientResponse<*>) = durationPredicate(t1)
            }

    @Provides
    @Singleton
    fun provideUriParser() = object : Function1<String, Uri> {
        override fun get(t1: String) = Uri.parse(t1)
    }

}
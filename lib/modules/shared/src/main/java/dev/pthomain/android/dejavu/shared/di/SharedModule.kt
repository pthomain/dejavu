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

package dev.pthomain.android.dejavu.shared.di

import android.content.Context
import android.net.Uri
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.shared.token.instruction.Hasher
import dev.pthomain.android.dejavu.shared.utils.Function1
import java.util.*
import javax.inject.Singleton

@Module
class SharedModule(
        private val context: Context,
        private val logger: Logger
) {

    @Provides
    @Singleton
    internal fun provideContext() = context.applicationContext

    @Provides
    @Singleton
    internal fun provideLogger() = logger

    @Provides
    @Singleton
    internal fun provideDateFactory() = object : Function1<Long?, Date> {
        override fun get(t1: Long?) = t1?.let { Date(it) } ?: Date()
    }

    @Provides
    @Singleton
    internal fun provideHasher(logger: Logger,
                               uriParser: Function1<String, Uri>) =
            Hasher(
                    logger,
                    uriParser::get
            )
}

object SilentLogger : Logger {
    override fun d(tagOrCaller: Any, message: String) = Unit
    override fun e(tagOrCaller: Any, message: String) = Unit
    override fun e(tagOrCaller: Any, t: Throwable, message: String?) = Unit
}
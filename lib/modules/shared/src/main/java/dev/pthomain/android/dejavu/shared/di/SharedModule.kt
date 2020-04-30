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
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.shared.token.instruction.Hasher
import dev.pthomain.android.dejavu.shared.utils.Function1
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

class SharedModule(
        private val context: Context,
        private val logger: Logger
) {
    val module = module {

        single { context.applicationContext }

        single { logger }

        single<Function1<Long?, Date>>(named("dateFactory")) {
            object : Function1<Long?, Date> {
                override fun get(t1: Long?) = if (t1 == null) Date() else Date(t1)
            }
        }

        single {
            Hasher(
                    get(),
                    get()
            )
        }
    }

}


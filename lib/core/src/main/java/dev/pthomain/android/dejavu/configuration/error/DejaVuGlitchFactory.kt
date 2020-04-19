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

package dev.pthomain.android.dejavu.configuration.error

import dev.pthomain.android.dejavu.retrofit.annotations.processor.CacheException
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.glitch.ErrorCode.CONFIG
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch.Companion.NON_HTTP_STATUS
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory

/**
 * Default implementation of ErrorFactory handling some usual base exceptions.
 *
 * @see dev.pthomain.android.dejavu.configuration.DejaVu.Configuration.errorFactory for overriding this factory
 * @see Glitch
 */
//TODO deprecate or wrap
class DejaVuGlitchFactory(private val glitchFactory: GlitchFactory)
    : ErrorFactory<Glitch> by glitchFactory {

    /**
     * Converts a throwable to a Glitch, containing some metadata around the exception
     *
     * @param throwable the given throwable to make sense of
     * @return an instance of Glitch
     */
    override fun invoke(throwable: Throwable) =
            when (throwable) {
                is CacheException -> getConfigError(throwable)
                else -> glitchFactory(throwable)
            }

    /**
     * Converts an CacheException to a Glitch
     *
     * @param throwable the original exception
     * @return the converted Glitch
     */
    private fun getConfigError(throwable: CacheException) =
            Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    CONFIG,
                    "Configuration error"
            )

}

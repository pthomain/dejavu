/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

package dev.pthomain.android.dejavu.demo.gson


import com.google.gson.JsonParseException
import dev.pthomain.android.dejavu.interceptors.internal.error.ErrorCode.UNEXPECTED_RESPONSE
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch.Companion.NON_HTTP_STATUS
import dev.pthomain.android.dejavu.interceptors.internal.error.GlitchFactory

/**
 * Custom ErrorFactory implementation handling Gson specific errors
 */
class GsonGlitchFactory : GlitchFactory() {

    override fun getError(throwable: Throwable) =
            when (throwable) {
                is JsonParseException -> Glitch(
                        throwable,
                        NON_HTTP_STATUS,
                        UNEXPECTED_RESPONSE,
                        throwable.message
                )
                else -> super.getError(throwable)
            }

}

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

package dev.pthomain.android.dejavu.retrofit.operation

import okhttp3.Request
import okio.Buffer
import java.io.IOException

internal class RequestBodyConverter : (Request) -> String? {
    /**
     * Converts a request's body to String
     *
     * @param p1 the OkHttp request
     * @return the request's body as a String
     */
    override fun invoke(p1: Request) =
            try {
                Buffer().apply {
                    p1.newBuilder().build().body()?.writeTo(this)
                }.readUtf8()
            } catch (e: IOException) {
                null
            }

}

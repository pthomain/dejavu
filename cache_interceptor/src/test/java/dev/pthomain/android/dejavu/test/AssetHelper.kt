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

package dev.pthomain.android.dejavu.test

import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.dejavu.injection.integration.module.NOW
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult
import dev.pthomain.android.dejavu.interceptors.response.Response
import io.reactivex.Observable
import java.io.*

class AssetHelper(private val assetsFolder: String,
                  private val gson: Gson) {

    fun <R : Any> observeStubbedResponse(fileName: String,
                                         responseClass: Class<R>,
                                         cacheToken: RequestToken<Cache, R>)
            : Observable<out DejaVuResult<R>> =
            observeFile(fileName)
                    .map { gson.fromJson(it, responseClass) }
                    .map {
                        Response(
                                it,
                                with(cacheToken) {
                                    ResponseToken(
                                            instruction,
                                            FRESH,
                                            NOW,
                                            NOW,
                                            NOW
                                    )
                                },
                                CallDuration(0, 0, 0)
                        )
                    }

    fun observeFile(fileName: String): Observable<String> =
            File(assetsFolder + fileName).let {
                FileInputStream(it).useAndLogError({
                    Observable.just(fileToString(it))
                })
            }

    @Throws(IOException::class)
    private fun fileToString(inputStream: InputStream) =
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).useAndLogError({ reader ->
                val builder = StringBuilder()
                var line: String?
                do {
                    line = reader.readLine()
                    if (line != null) {
                        builder.append(line)
                        builder.append('\n')
                    }
                } while (line != null)
                builder.toString()
            })

}

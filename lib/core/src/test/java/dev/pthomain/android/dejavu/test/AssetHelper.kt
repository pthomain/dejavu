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

import kotlinx.serialization.json.Json

import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.di.integration.module.NOW
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.*

class AssetHelper(private val assetsFolder: String,
                  private val json: Json = Json { ignoreUnknownKeys = true }) {

    fun <R : Any> flowStubbedResponse(fileName: String,
                                      responseClass: Class<R>,
                                      cacheToken: RequestToken<Cache, R>)
            : Flow<DejaVuResult<R>> =
            flowFile(fileName)
                    .map { json.decodeFromString(kotlinx.serialization.serializer(responseClass), it) as R }
                    .map {
                        Response(
                                it,
                                with(cacheToken) {
                                    ResponseToken(
                                            instruction,
                                            FRESH,
                                            NOW,
                                            NOW
                                    )
                                },
                                CallDuration(0, 0, 0)
                        )
                    }

    fun flowFile(fileName: String): Flow<String> =
            File(assetsFolder + fileName).let { file ->
                flow {
                    FileInputStream(file).use { stream ->
                        emit(fileToString(stream))
                    }
                }
            }

    @Throws(IOException::class)
    private fun fileToString(inputStream: InputStream) =
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
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
            }

}

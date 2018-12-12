/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.test

import com.google.gson.Gson
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import java.io.*

class AssetHelper(private val assetsFolder: String,
                  private val gson: Gson,
                  private val logger: Logger) {

    fun <E, R> getStubbedResponse(fileName: String,
                                  responseClass: Class<R>)
            : Observable<R> where E : Exception,
                                  E : NetworkErrorProvider,
                                  R : CacheMetadata.Holder<E> {
        return observeFile(fileName).map { json -> gson.fromJson(json, responseClass) }
    }

    internal fun observeFile(fileName: String): Observable<String> {
        var inputStream: InputStream? = null

        try {
            try {
                val file = File(assetsFolder + fileName)
                inputStream = FileInputStream(file)
                return Observable.just(fileToString(inputStream))
            } finally {
                inputStream?.close()
            }
        } catch (e: IOException) {
            val message = ("An error occurred while trying to read "
                    + "file: "
                    + fileName)
            logger.e(
                    this,
                    e,
                    message
            )
            return Observable.error(ApiError(
                    e,
                    ApiError.NON_HTTP_STATUS,
                    ErrorCode.UNKNOWN,
                    message
            ))
        }

    }

    companion object {

        @Throws(IOException::class)
        internal fun fileToString(inputStream: InputStream): String {
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            reader.use { reader ->
                val builder = StringBuilder()
                var line: String?
                do {
                    line = reader.readLine()
                    builder.append(line)
                    builder.append('\n')
                } while (line != null)

                return builder.toString()
            }
        }
    }

}

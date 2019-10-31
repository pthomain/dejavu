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

package dev.pthomain.android.dejavu.test

import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import io.reactivex.Observable
import java.io.*

class AssetHelper(private val assetsFolder: String,
                  private val gson: Gson,
                  private val errorFactory: ErrorFactory<Glitch>) {

    fun <R> observeStubbedResponse(fileName: String,
                                   responseClass: Class<R>,
                                   cacheToken: CacheToken)
            : Observable<R> where R : CacheMetadata.Holder<Glitch> =
            observeFile(fileName)
                    .map { gson.fromJson(it, responseClass) }
                    .doOnNext {
                        it.metadata = errorFactory.newMetadata(cacheToken)
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

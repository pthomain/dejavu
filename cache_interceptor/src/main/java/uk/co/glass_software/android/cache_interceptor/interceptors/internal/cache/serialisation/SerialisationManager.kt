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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation

import com.google.gson.Gson
import org.iq80.snappy.Snappy
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager

internal class SerialisationManager<E>(private val logger: Logger,
                                       private val encryptionManager: EncryptionManager?,
                                       private val gson: Gson)
        where E : Exception,
              E : NetworkErrorProvider {

    fun deserialise(instructionToken: CacheToken,
                    data: ByteArray,
                    isEncrypted: Boolean,
                    isCompressed: Boolean,
                    onError: () -> Unit): ResponseWrapper<E>? {
        val responseClass = instructionToken.instruction.responseClass
        val simpleName = responseClass.simpleName

        try {
            var uncompressed = if (isCompressed)
                Snappy.uncompress(data, 0, data.size).apply {
                    logCompression(data, simpleName, this)
                }
            else data

            if (isEncrypted && encryptionManager != null) {
                uncompressed = encryptionManager.decryptBytes(uncompressed, DATA_TAG)
            }

            val response = gson.fromJson(String(uncompressed), responseClass)

            return ResponseWrapper(
                    responseClass,
                    response,
                    CacheMetadata<E>(instructionToken, null)
            )
        } catch (e: Exception) {
            logger.e(
                    this,
                    e,
                    "Could not deserialise $simpleName: clearing the cache"
            )
            onError()
            return null
        }
    }

    fun serialise(responseWrapper: ResponseWrapper<E>,
                  encryptData: Boolean,
                  compressData: Boolean): ByteArray? {
        val simpleName = responseWrapper.responseClass.simpleName
        val response = responseWrapper.response!!

        return gson.toJson(response)
                .toByteArray()
                .let {
                    if (encryptData && encryptionManager != null) encryptionManager.encryptBytes(it, DATA_TAG)
                    else it
                }
                ?.let {
                    if (compressData) Snappy.compress(it).also { compressed ->
                        logCompression(compressed, simpleName, it)
                    }
                    else it
                }
    }

    private fun logCompression(compressedData: ByteArray,
                               simpleName: String,
                               uncompressed: ByteArray) {
        logger.d(
                this,
                "Compressed/uncompressed $simpleName: ${compressedData.size}B/${uncompressed.size}B "
                        + "(${100 * compressedData.size / uncompressed.size}%)"
        )
    }

    companion object {
        private const val DATA_TAG = "DATA_TAG"
    }
}

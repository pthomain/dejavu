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

package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser

internal class SerialisationManager<E>(private val logger: Logger,
                                       private val byteToStringConverter: (ByteArray) -> String,
                                       private val encryptionManager: EncryptionManager?,
                                       private val compresser: (ByteArray) -> ByteArray,
                                       private val uncompresser: (ByteArray, Int, Int) -> ByteArray,
                                       private val serialiser: Serialiser)
        where E : Exception,
              E : NetworkErrorProvider {

    fun serialise(responseWrapper: ResponseWrapper<E>,
                  encryptData: Boolean,
                  compressData: Boolean) =
            if (serialiser.canHandleType(responseWrapper.responseClass)) {
                serialiser.serialise(responseWrapper.response!!)
                        .toByteArray()
                        .let {
                            if (encryptData && encryptionManager != null)
                                encryptionManager.encryptBytes(it, DATA_TAG)
                            else it
                        }
                        ?.let {
                            if (compressData) compresser(it).also { compressed ->
                                logCompression(
                                        compressed,
                                        responseWrapper.responseClass.simpleName,
                                        it
                                )
                            }
                            else it
                        }
            } else null

    fun deserialise(instructionToken: CacheToken,
                    data: ByteArray,
                    isEncrypted: Boolean,
                    isCompressed: Boolean,
                    onError: () -> Unit): ResponseWrapper<E>? {
        val responseClass = instructionToken.instruction.responseClass
        val simpleName = responseClass.simpleName

        try {
            return (if (isCompressed) {
                uncompresser(data, 0, data.size).also {
                    logCompression(data, simpleName, it)
                }
            } else {
                data
            }).let {
                if (isEncrypted && encryptionManager != null)
                    encryptionManager.decryptBytes(it, DATA_TAG)
                            ?: throw IllegalStateException("Could not decrypt data")
                else it
            }.let {
                serialiser.deserialise(byteToStringConverter(it), responseClass)
            }.let {
                ResponseWrapper(
                        responseClass,
                        it,
                        CacheMetadata<E>(instructionToken, null)
                )
            }
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

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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder.Incomplete

/**
 * Provides methods handling the serialisation and deserialisation of the required cache metadata
 * to be used as a File name for the purpose of filtering and querying of the cached responses.
 */
internal class FileNameSerialiser {

    /**
     * Serialises the required cache metadata to be used as a File name for the purpose of
     * filtering and querying of the cached responses.
     *
     * @param cacheDataHolder the model containing the metadata required for the name serialisation
     * @return the file name to use to save the response associated to the given metadata
     */
    fun serialise(cacheDataHolder: CacheDataHolder.Complete) =
            with(cacheDataHolder) {
                listOf(
                        requestMetadata.urlHash,
                        cacheDate.toString(),
                        expiryDate.toString(),
                        requestMetadata.classHash,
                        ifElse(isCompressed, "1", "0"),
                        ifElse(isEncrypted, "1", "0")
                ).joinToString(SEPARATOR)
            }

    /**
     * Partially deserialises the given file name to the associated cache metadata.
     * The returned holder cannot be used as metadata to a deserialised response as it is missing
     * the required request metadata. Instead, this holder can be used solely for the purpose
     * of filtering the locally saved responses based on the existing information contained in the
     * file name.
     *
     * @param fileName the local file name
     *
     * @return an incomplete holder to be used for filtering
     * @throws SerialisationException if the given file name is invalid
     */
    @Throws(SerialisationException::class)
    fun deserialise(fileName: String) =
            if (isValidFormat(fileName)) {
                with(fileName.split(SEPARATOR)) {
                    Incomplete(
                            get(1).toLong(),
                            get(2).toLong(),
                            ByteArray(0),
                            get(3),
                            get(4) == "1",
                            get(5) == "1"
                    )
                }
            } else throw SerialisationException("This file name is invalid: $fileName")

    /**
     * Fully deserialises the given file name and request metadata to the associated cache metadata.
     * The returned holder can be used to populate the metadata of the associated deserialised response.
     *
     * @param requestMetadata the optional request metadata needed to fully deserialise the file name.
     * @param fileName the local file name
     *
     * @return the complete holder to be used for the deserialised response metadata
     * @throws SerialisationException if the given file name is invalid
     */
    @Throws(SerialisationException::class)
    fun deserialise(requestMetadata: RequestMetadata.Hashed,
                    fileName: String) =
            with(deserialise(fileName)) {
                CacheDataHolder.Complete(
                        requestMetadata,
                        cacheDate,
                        expiryDate,
                        data,
                        responseClassHash,
                        isCompressed,
                        isEncrypted
                )
            }

    companion object {
        const val SEPARATOR = "_"

        private val validFileRegex = Regex("^([^_]+_){5}[^_]+\$")

        /**
         * @return whether or not the given file name has the format of a serialised cache entry.
         */
        fun isValidFormat(name: String) = validFileRegex.matches(name)
    }
}

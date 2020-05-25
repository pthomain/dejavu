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

package dev.pthomain.android.dejavu.persistence.base.store

import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.persistence.Persisted
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import java.util.*

/**
 * Provides methods handling the serialisation and deserialisation of the required cache metadata
 * to be used as a File name for the purpose of filtering and querying of the cached responses.
 */
class KeySerialiser(private val dateFactory: (Long?) -> Date) {

    /**
     * Serialises the required cache metadata to be used as a File name for the purpose of
     * filtering and querying of the cached responses.
     *
     * @param persistedData the model containing the metadata required for the name serialisation
     * @return the file name to use to save the response associated to the given metadata
     */
    fun <R : Any> serialise(token: ResponseToken<Cache, R>) =
            with(token.instruction.requestMetadata) {
                listOf(
                        requestHash,
                        classHash,
                        token.requestDate.time,
                        token.expiryDate!!.time,
                        with(token.instruction.operation.serialisation) {
                            if (isBlank()) "0" else this
                        }
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
                    Persisted.Key(
                            get(0),
                            get(1),
                            dateFactory(get(2).toLong()),
                            dateFactory(get(3).toLong()),
                            with(get(4)) {
                                if (this == "0") "" else this
                            }
                    )
                }
            } else throw SerialisationException("This file name is invalid: $fileName")

    companion object {
        const val SEPARATOR = "_"

        private val validFileRegex = Regex("^([^_]+_){4}[^_]+\$")

        /**
         * @return whether or not the given file name has the format of a serialised cache entry.
         */
        fun isValidFormat(name: String) = validFileRegex.matches(name)
    }
}

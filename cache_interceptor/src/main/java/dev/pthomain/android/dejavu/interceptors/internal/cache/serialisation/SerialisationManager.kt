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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation

import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FilePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.response.ResponseWrapper
import java.util.*

/**
 * TODO + test
 */
internal class SerialisationManager<E> private constructor(private val serialiser: Serialiser,
                                                           private val byteToStringConverter: (ByteArray) -> String,
                                                           private val decoratorList: List<SerialisationDecorator<E>>)
        where E : Exception,
              E : NetworkErrorPredicate {

    private val reversedDecoratorList = decoratorList.reversed()
    private val nullException = SerialisationException(
            "Could not serialise the given response",
            NullPointerException("The response was null")
    )

    @Throws(SerialisationException::class)
    fun serialise(responseWrapper: ResponseWrapper<E>,
                  metadata: SerialisationDecorationMetadata): ByteArray {
        val response = responseWrapper.response
        val responseClass = responseWrapper.responseClass

        return if (response == null) throw nullException
        else when {
            responseClass == String::class.java -> (response as String)
            !serialiser.canHandleType(response.javaClass) -> throw nullException
            else -> serialiser.serialise(response)
        }.let {
            var serialised = it.toByteArray()

            decoratorList.forEach {
                serialised = it.decorateSerialisation(
                        responseWrapper,
                        metadata,
                        serialised
                )
            }

            serialised
        }
    }

    @Throws(SerialisationException::class)
    fun deserialise(instructionToken: CacheToken,
                    data: ByteArray,
                    metadata: SerialisationDecorationMetadata): ResponseWrapper<E> {
        val responseClass = instructionToken.instruction.responseClass
        var deserialised = data

        reversedDecoratorList.forEach {
            deserialised = it.decorateDeserialisation(
                    instructionToken,
                    metadata,
                    deserialised
            )
        }

        return byteToStringConverter(deserialised)
                .let { serialiser.deserialise(it, responseClass) }
                .let {
                    ResponseWrapper(
                            responseClass,
                            it,
                            CacheMetadata<E>(instructionToken, null)
                    )
                }
    }

    class Factory<E>(private val serialiser: Serialiser,
                     private val byteToStringConverter: (ByteArray) -> String,
                     private val decoratorList: LinkedList<SerialisationDecorator<E>>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun <P : PersistenceManager<E>> create(persistenceManagerClass: Class<P>): SerialisationManager<E> {
            val filteredList = if (persistenceManagerClass == FilePersistenceManager::class.java)
                decoratorList
            else
                decoratorList.filter { it !is FileSerialisationDecorator<E> }

            return SerialisationManager(
                    serialiser,
                    byteToStringConverter,
                    filteredList
            )
        }
    }
}

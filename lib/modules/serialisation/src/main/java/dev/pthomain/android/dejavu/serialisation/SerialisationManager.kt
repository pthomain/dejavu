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

package dev.pthomain.android.dejavu.serialisation

import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.configuration.SerialisationException
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*

/**
 * Handles the different steps of the serialisation of the response.
 *
 * @param errorFactory the factory converting throwables to custom exceptions
 * @param serialiser instance of Serialiser in charge of serialising a model to a String (typically JSON)
 * @param byteToStringConverter a factory converting a ByteArray to a String
 * @param decoratorList a list of SerialisationDecorator to be applied recursively during the serialisation process
 */
class SerialisationManager<E> internal constructor(
        private val errorFactory: ErrorFactory<E>,
        private val dateFactory: (Long?) -> Date,
        private val serialiser: Serialiser,
        private val byteToStringConverter: (ByteArray) -> String,
        private val decoratorList: List<SerialisationDecorator<E>>
) where E : Throwable,
        E : NetworkErrorPredicate {

    private val reversedDecoratorList = decoratorList.reversed()

    /**
     * Serialises the given wrapper's response first to a String using the provided Serialiser,
     * then to a ByteArray and then recursively applies the list of decorators on this array.
     *
     * @param responseWrapper the wrapper containing the response to serialise
     * @param metadata the overall metadata associated with the current serialisation
     * @return the serialised response as a ByteArray
     * @throws SerialisationException in case the serialisation fails
     */
    @Throws(SerialisationException::class)
    fun <R : Any> serialise(
            responseWrapper: Response<R, Cache>,
            metadata: SerialisationDecorationMetadata
    ): ByteArray {
        val response = responseWrapper.response
        val responseClass = responseWrapper.cacheToken.instruction.requestMetadata.responseClass

        return when {
            responseClass == String::class.java -> (response as String)

            !serialiser.canHandleType(response.javaClass) -> throw SerialisationException(
                    "Could not serialise the given response"
            )

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

    /**
     * Deserialises the given payload first to a ByteArray using the provided Serialiser,
     * then recursively applies the list of decorators on this array, in the reverse order used
     * in the initial serialisation step.
     *
     * @param instructionToken the request's instruction token
     * @param metadata the overall metadata associated with the current deserialisation
     * @param data the payload being deserialised
     * @return the deserialised response wrapped in a ResponseWrapper
     * @throws SerialisationException in case the deserialisation fails
     */
    @Throws(SerialisationException::class)
    fun <R : Any> deserialise(
            instructionToken: CacheToken<Cache, R>,
            data: ByteArray,
            metadata: SerialisationDecorationMetadata
    ): R {
        val requestMetadata = instructionToken.instruction.requestMetadata
        val responseClass = requestMetadata.responseClass
        var deserialised = data

        reversedDecoratorList.forEach {
            deserialised = it.decorateDeserialisation(
                    instructionToken,
                    metadata,
                    deserialised
            )
        }

        return serialiser.deserialise(
                byteToStringConverter(deserialised),
                responseClass
        )
    }
}
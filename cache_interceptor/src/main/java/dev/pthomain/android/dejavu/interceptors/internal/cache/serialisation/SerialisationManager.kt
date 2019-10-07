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

import dev.pthomain.android.boilerplate.core.utils.lambda.Action
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.response.ResponseWrapper
import java.util.*

/**
 * TODO + test
 */
internal class SerialisationManager<E>(private val logger: Logger,
                                       private val serialiser: Serialiser,
                                       private val byteToStringConverter: (ByteArray) -> String,
                                       private val decoratorList: LinkedList<SerialisationDecorator<E>>)
        where E : Exception,
              E : NetworkErrorPredicate {

    private val reversedDecoratorList = decoratorList.reversed()

    fun serialise(responseWrapper: ResponseWrapper<E>,
                  encryptData: Boolean,
                  compressData: Boolean): ByteArray? {
        val response = responseWrapper.response
        val responseClass = responseWrapper.responseClass
        val metadata = SerialisationDecorationMetadata(
                compressData,
                encryptData
        )

        var serialised = when {
            responseClass == String::class.java -> response?.let { it as String }
            response == null || !serialiser.canHandleType(response.javaClass) -> null
            else -> serialiser.serialise(response)
        }?.toByteArray()

        decoratorList.forEach {
            serialised = it.decorateSerialisation(
                    responseWrapper,
                    metadata,
                    serialised
            )
        }

        return serialised
    }

    fun deserialise(instructionToken: CacheToken,
                    data: ByteArray,
                    isEncrypted: Boolean,
                    isCompressed: Boolean,
                    onError: Action): ResponseWrapper<E>? {
        val responseClass = instructionToken.instruction.responseClass
        val simpleName = responseClass.simpleName
        val metadata = SerialisationDecorationMetadata(
                isCompressed,
                isEncrypted
        )

        var deserialised: ByteArray? = data

        reversedDecoratorList.forEach {
            deserialised = it.decorateDeserialisation(
                    instructionToken,
                    metadata,
                    deserialised
            )
        }

        return deserialised
                ?.let { byteToStringConverter(it) }
                ?.let { serialiser.deserialise(it, responseClass) }
                ?.let {
                    ResponseWrapper(
                            responseClass,
                            it,
                            CacheMetadata<E>(instructionToken, null)
                    )
                }.apply {
                    if (this == null) {
                        logger.e(
                                this@SerialisationManager,
                                "Could not deserialise $simpleName: clearing the cache"
                        )
                        onError()
                    }
                }
    }

}

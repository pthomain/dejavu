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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.file

import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.response.ResponseWrapper

/**
 * Optional encryption step of the serialisation process
 *
 * @param encryptionManager an instance of EncryptionManager
 * @see dev.pthomain.android.dejavu.configuration.CacheConfiguration.Builder.encryption
 */
class FileSerialisationDecorator<E>(private val byteToStringConverter: (ByteArray) -> String)
    : SerialisationDecorator<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    //TODO
    @Throws(SerialisationException::class)
    override fun decorateSerialisation(responseWrapper: ResponseWrapper<E>,
                                       metadata: SerialisationDecorationMetadata,
                                       payload: ByteArray) =
            responseWrapper.responseClass.name
                    .plus("\n")
                    .plus(byteToStringConverter(payload))
                    .toByteArray()

    @Throws(SerialisationException::class)
    override fun decorateDeserialisation(instructionToken: CacheToken,
                                         metadata: SerialisationDecorationMetadata,
                                         payload: ByteArray) =
            byteToStringConverter(payload).let {
                if (it.contains("\n")) it.substring(it.indexOf("\n"))
                else throw SerialisationException("Could not extract the payload")
            }.toByteArray()

}
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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.encryption

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.base.EncryptionManager

/**
 * Optional encryption step of the serialisation process
 *
 * @param encryptionManager an instance of EncryptionManager
 * @see dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.Builder.withEncryption
 */
internal class EncryptionSerialisationDecorator<E>(private val encryptionManager: EncryptionManager)
    : SerialisationDecorator<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Implements optional encryption during the serialisation process.
     *
     * @param responseWrapper the wrapper associated with the payload being serialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the encrypted payload
     * @throws SerialisationException in case this encryption step failed
     */
    @Throws(SerialisationException::class)
    override fun decorateSerialisation(responseWrapper: ResponseWrapper<Cache, RequestToken<Cache>, E>,
                                       metadata: SerialisationDecorationMetadata,
                                       payload: ByteArray) =
            if (metadata.isEncrypted && encryptionManager.isEncryptionAvailable) {
                encryptionManager.encryptBytes(payload, DATA_TAG)
                        ?: throw SerialisationException("Could not encrypt data")
            } else payload

    /**
     * Implements optional decryption during the deserialisation process.
     *
     * @param instructionToken the request's instruction token associated with the payload being deserialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the decrypted payload
     * @throws SerialisationException in case this decryption step failed
     */
    @Throws(SerialisationException::class)
    override fun decorateDeserialisation(instructionToken: InstructionToken<Cache>,
                                         metadata: SerialisationDecorationMetadata,
                                         payload: ByteArray) =
            if (metadata.isEncrypted && encryptionManager.isEncryptionAvailable) {
                encryptionManager.decryptBytes(payload, DATA_TAG)
                        ?: throw SerialisationException("Could not decrypt data")
            } else payload

    companion object {
        internal const val DATA_TAG = "DATA_TAG"
    }

}
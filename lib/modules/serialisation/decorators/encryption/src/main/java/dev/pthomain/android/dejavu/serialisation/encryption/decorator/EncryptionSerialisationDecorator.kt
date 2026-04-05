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

package dev.pthomain.android.dejavu.serialisation.encryption.decorator

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.encryption.TinkEncryptionManager

/**
 * Optional encryption step of the serialisation process using Google Tink.
 *
 * @param encryptionManager an instance of TinkEncryptionManager
 */
internal class EncryptionSerialisationDecorator(
        private val encryptionManager: TinkEncryptionManager
) : SerialisationDecorator {

    override val uniqueName = "ENCRYPT"

    /**
     * Implements optional encryption during the serialisation process.
     *
     * @param responseClass the class of the response being serialised
     * @param operation the cache operation
     * @param payload the payload being serialised
     * @return the encrypted payload
     * @throws SerialisationException in case this encryption step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateSerialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ): ByteArray =
            try {
                encryptionManager.encrypt(payload, DATA_TAG.toByteArray())
            } catch (e: Exception) {
                throw SerialisationException("Could not encrypt data", e)
            }

    /**
     * Implements optional decryption during the deserialisation process.
     *
     * @param responseClass the class of the response being deserialised
     * @param operation the cache operation
     * @param payload the payload being deserialised
     * @return the decrypted payload
     * @throws SerialisationException in case this decryption step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateDeserialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ): ByteArray =
            try {
                encryptionManager.decrypt(payload, DATA_TAG.toByteArray())
            } catch (e: Exception) {
                throw SerialisationException("Could not decrypt data", e)
            }

    companion object {
        internal const val DATA_TAG = "DATA_TAG"
    }
}

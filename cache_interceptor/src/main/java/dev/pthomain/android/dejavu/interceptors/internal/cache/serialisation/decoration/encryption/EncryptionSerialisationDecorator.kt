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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.encryption

import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.mumbo.base.EncryptionManager

//TODO JavaDoc + test
internal class EncryptionSerialisationDecorator<E>(
        private val encryptionManager: EncryptionManager
) : SerialisationDecorator<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    override fun decorateSerialisation(responseWrapper: ResponseWrapper<E>,
                                       metadata: SerialisationDecorationMetadata,
                                       payload: ByteArray?) =
            if (metadata.isEncrypted && encryptionManager.isEncryptionAvailable) {
                encryptionManager.encryptBytes(payload, DATA_TAG)
            } else payload

    override fun decorateDeserialisation(instructionToken: CacheToken,
                                         metadata: SerialisationDecorationMetadata,
                                         payload: ByteArray?) =
            if (metadata.isEncrypted && encryptionManager.isEncryptionAvailable) {
                encryptionManager.decryptBytes(payload, DATA_TAG)
                        ?: throw IllegalStateException("Could not decrypt data")
            } else payload

    companion object {
        private const val DATA_TAG = "DATA_TAG"
    }

}
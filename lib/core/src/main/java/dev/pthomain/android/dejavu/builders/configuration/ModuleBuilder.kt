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

package dev.pthomain.android.dejavu.builders.configuration

import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.base.EncryptionManager

interface ModuleBuilder<E, B : ModuleBuilder<E, B>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Provide a different PersistenceManager to handle the persistence of the cached requests.
     * The default implementation is FilePersistenceManager which saves the responses to
     * the filesystem.
     *
     * @see dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
     * @see dev.pthomain.android.dejavu.interceptors.cache.persistence.database.DatabasePersistenceManager
     *
     * @param persistenceManager the PersistenceManager implementation to override the default one
     */
    fun withPersistence(persistenceManager: PersistenceManager<E>): B

    /**
     * Sets the EncryptionManager implementation. Can be used to provide a custom implementation
     * or to choose one provided by the Mumbo library. For compatibility reasons, the default is
     * Facebook Conceal, but apps targeting API 23+ should use Tink (JetPack).
     *
     * @param mumboPicker picker for the encryption implementation, with a choice of:
     * - Facebook's Conceal for API levels < 23 (see https://facebook.github.io/conceal)
     * - AndroidX's JetPack Security (Tink) implementation for API level >= 23 only (see https://developer.android.com/jetpack/androidx/releases/security)
     * - custom implementation using the EncryptionManager interface
     *
     * NB: if you are targeting API level 23 or above, you should use Tink as it is a more secure implementation.
     * However if your API level target is less than 23, using Tink will trigger a runtime exception.
     */
    fun withEncryption(encryptionManager: EncryptionManager): B

    enum class PersistenceType {
        FILE,
        MEMORY,
        SQLITE
    }
}
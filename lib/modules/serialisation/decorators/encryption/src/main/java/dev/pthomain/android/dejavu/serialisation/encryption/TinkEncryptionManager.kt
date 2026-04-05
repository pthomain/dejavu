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

package dev.pthomain.android.dejavu.serialisation.encryption

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/**
 * Encryption manager backed by Google Tink's AEAD primitive.
 *
 * Uses AES-256-GCM with a master key stored in the Android Keystore.
 */
class TinkEncryptionManager(context: Context) {

    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "dejavu_keyset", "dejavu_encryption_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://dejavu_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    /**
     * Encrypts the given data using AES-256-GCM.
     *
     * @param data the plaintext data
     * @param associatedData optional associated data for AEAD
     * @return the ciphertext
     */
    fun encrypt(data: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray =
        aead.encrypt(data, associatedData)

    /**
     * Decrypts the given data using AES-256-GCM.
     *
     * @param data the ciphertext
     * @param associatedData optional associated data for AEAD
     * @return the plaintext
     */
    fun decrypt(data: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray =
        aead.decrypt(data, associatedData)
}

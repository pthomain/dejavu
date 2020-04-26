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

import dagger.Provides
import dev.pthomain.android.dejavu.serialisation.di.SerialisationComponent
import dev.pthomain.android.dejavu.serialisation.encryption.decorator.EncryptionSerialisationDecorator
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.mumbo.base.EncryptionManager
import javax.inject.Singleton

object Encryption {

    class Builder(encryptionManager: EncryptionManager)
        : Component by DaggerEncryption_Component
            .builder()
            .module(Module(encryptionManager))
            .build()

    @Singleton
    @dagger.Component(modules = [Module::class])
    internal interface Component : SerialisationComponent

    @dagger.Module
    internal class Module(
            private val encryptionManager: EncryptionManager
    ) {

        @Provides
        @Singleton
        internal fun provideEncryptionSerialisationDecorator(): SerialisationDecorator =
                EncryptionSerialisationDecorator(
                        encryptionManager
                )

    }
}

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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation

import android.net.Uri
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.injection.Function3
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.compression.CompressionSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.encryption.EncryptionSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.base.EncryptionManager
import org.iq80.snappy.Snappy
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class SerialisationModule<E> where E : Exception,
                                                     E : NetworkErrorPredicate {
    @Provides
    @Singleton
    fun provideSerialiser(configuration: DejaVuConfiguration<E>) =
            configuration.serialiser

    @Provides
    @Singleton
    fun provideEncryptionManager(configuration: DejaVuConfiguration<E>) =
            configuration.encryptionManager

    @Provides
    @Singleton
    fun provideCompresser() = object : Function1<ByteArray, ByteArray> {
        override fun get(t1: ByteArray) = Snappy.compress(t1)
    }

    @Provides
    @Singleton
    fun provideUncompresser() = object : Function3<ByteArray, Int, Int, ByteArray> {
        override fun get(t1: ByteArray, t2: Int, t3: Int) = Snappy.uncompress(t1, t2, t3)
    }


    @Provides
    @Singleton
    fun provideByteToStringConverter() = object : Function1<ByteArray, String> {
        override fun get(t1: ByteArray) = String(t1)
    }

    @Provides
    @Singleton
    fun provideFileNameSerialiser() =
            FileNameSerialiser()

    @Provides
    @Singleton
    fun provideHasher(logger: Logger,
                      uriParser: Function1<String, Uri>) =
            Hasher.Factory(
                    logger,
                    uriParser::get
            ).create()

    @Provides
    @Singleton
    fun provideFileSerialisationDecorator(byteToStringConverter: Function1<ByteArray, String>) =
            FileSerialisationDecorator<E>(byteToStringConverter::get)

    @Provides
    @Singleton
    fun provideCompressionSerialisationDecorator(logger: Logger,
                                                 compresser: Function1<ByteArray, ByteArray>,
                                                 uncompresser: Function3<ByteArray, Int, Int, ByteArray>) =
            CompressionSerialisationDecorator<E>(
                    logger,
                    compresser::get,
                    uncompresser::get
            )

    @Provides
    @Singleton
    fun provideEncryptionSerialisationDecorator(encryptionManager: EncryptionManager) =
            EncryptionSerialisationDecorator<E>(encryptionManager)

    @Provides
    @Singleton
    fun provideSerialisationManagerFactory(configuration: DejaVuConfiguration<E>,
                                           byteToStringConverter: Function1<ByteArray, String>,
                                           dateFactory: Function1<Long?, Date>,
                                           fileSerialisationDecorator: FileSerialisationDecorator<E>,
                                           compressionSerialisationDecorator: CompressionSerialisationDecorator<E>,
                                           encryptionSerialisationDecorator: EncryptionSerialisationDecorator<E>) =
            SerialisationManager.Factory(
                    configuration.serialiser,
                    configuration.errorFactory,
                    dateFactory::get,
                    byteToStringConverter::get,
                    fileSerialisationDecorator,
                    compressionSerialisationDecorator,
                    encryptionSerialisationDecorator
            )

}
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

package dev.pthomain.android.dejavu.interceptors.cache

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.injection.module.Function1
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import io.reactivex.subjects.PublishSubject
import java.util.*
import javax.inject.Singleton


@Module
internal class CacheModule<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideCacheMetadataManager(configuration: DejaVuConfiguration<E>,
                                    logger: Logger,
                                    persistenceManager: PersistenceManager<E>,
                                    dateFactory: Function1<Long?, Date>) =
            CacheMetadataManager(
                    configuration.errorFactory,
                    persistenceManager,
                    dateFactory::get,
                    configuration.cacheDurationInMillis,
                    logger
            )

    @Provides
    @Singleton
    fun provideCacheManager(logger: Logger,
                            persistenceManager: PersistenceManager<E>,
                            cacheMetadataManager: CacheMetadataManager<E>,
                            dateFactory: Function1<Long?, Date>,
                            emptyResponseFactory: EmptyResponseFactory<E>) =
            CacheManager(
                    persistenceManager,
                    cacheMetadataManager,
                    emptyResponseFactory,
                    dateFactory::get,
                    logger
            )

    @Provides
    @Singleton
    fun provideCacheMetadataSubject() =
            PublishSubject.create<CacheMetadata<E>>()

    @Provides
    @Singleton
    fun provideCacheMetadataObservable(subject: PublishSubject<CacheMetadata<E>>) =
            subject.map { it }!!

}

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

package dev.pthomain.android.dejavu.interceptors.cache

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.subjects.PublishSubject
import java.util.*
import javax.inject.Singleton


@Module
internal abstract class CacheModule<E>
        where E : Throwable,
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
                    configuration.durationPredicate,
                    logger
            )

    @Provides
    @Singleton
    fun provideCacheManager(logger: Logger,
                            persistenceManager: PersistenceManager<E>,
                            cacheMetadataManager: CacheMetadataManager<E>,
                            dateFactory: Function1<Long?, Date>,
                            emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>) =
            CacheManager(
                    persistenceManager,
                    cacheMetadataManager,
                    emptyResponseWrapperFactory,
                    dateFactory::get,
                    logger
            )

    @Provides
    @Singleton
    fun provideDejaVuResultSubject() =
            PublishSubject.create<DejaVuResult<*>>()

    @Provides
    @Singleton
    fun provideDejaVuResultObservable(subject: PublishSubject<DejaVuResult<*>>) =
            subject.map { it }!!

}

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

package dev.pthomain.android.dejavu.cache

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.utils.Function1
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*
import javax.inject.Singleton


@Module
abstract class CacheModule<E>(
        private val persistenceManager: PersistenceManager
) where E : Throwable,
        E : NetworkErrorPredicate {

    @Provides
    @Singleton
    internal fun provideCacheMetadataManager(
            logger: Logger,
            errorFactory: ErrorFactory<E>,
            durationPredicate: Function1<TransientResponse<*>, Int?>,
            dateFactory: Function1<Long?, Date>
    ) =
            CacheMetadataManager(
                    errorFactory,
                    persistenceManager,
                    dateFactory::get,
                    durationPredicate::get,
                    logger
            )

    @Provides
    @Singleton
    internal fun provideCacheManager(
            logger: Logger,
            cacheMetadataManager: CacheMetadataManager<E>,
            dateFactory: Function1<Long?, Date>,
            emptyResponseFactory: EmptyResponseFactory<E>
    ) =
            CacheManager(
                    persistenceManager,
                    cacheMetadataManager,
                    emptyResponseFactory,
                    dateFactory::get,
                    logger
            )

}

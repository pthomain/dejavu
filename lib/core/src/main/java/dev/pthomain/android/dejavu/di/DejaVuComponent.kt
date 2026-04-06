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

package dev.pthomain.android.dejavu.di

import android.content.Context
import android.net.Uri
import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.cache.CacheManager
import dev.pthomain.android.dejavu.cache.CacheMetadataManager
import dev.pthomain.android.dejavu.cache.metadata.response.TransientResponse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.serialisation.SerialisationArgumentValidator
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.error.ErrorFactory
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import java.util.*

/**
 * Manual dependency injection component that constructs the entire DejaVu dependency graph.
 * Replaces the previous Koin-based DejaVuModule.
 */
class DejaVuComponent<E>(
        val context: Context,
        val logger: Logger,
        val errorFactory: ErrorFactory<E>,
        val persistenceManager: PersistenceManager,
        val decorators: List<SerialisationDecorator>,
        val operationPredicate: (RequestMetadata<*>) -> Remote?,
        val durationPredicate: (TransientResponse<*>) -> Int?,
        val dateFactory: DateFactory
) where E : Throwable,
        E : NetworkErrorPredicate {

    val uriParser: (String) -> Uri = Uri::parse

    internal val hasher = Hasher(
            logger,
            uriParser
    )

    internal val networkInterceptorFactory = NetworkInterceptor.Factory<E>(
            context,
            logger,
            dateFactory
    )

    internal val cacheMetadataManager = CacheMetadataManager<E>(
            errorFactory,
            persistenceManager,
            dateFactory,
            durationPredicate::invoke,
            logger
    )

    internal val emptyResponseFactory = EmptyResponseFactory<E>(
            errorFactory,
            dateFactory
    )

    internal val cacheManager = CacheManager<E>(
            persistenceManager,
            cacheMetadataManager,
            emptyResponseFactory,
            dateFactory,
            logger
    )

    internal val cacheInterceptorFactory = CacheInterceptor.Factory<E>(
            cacheManager
    )

    internal val responseInterceptorFactory = ResponseInterceptor.Factory<E>(
            logger,
            dateFactory
    )

    val serialisationArgumentValidator = SerialisationArgumentValidator(decorators)

    val interceptorFactory = DejaVuInterceptor.Factory<E>(
            hasher,
            logger,
            dateFactory,
            serialisationArgumentValidator,
            networkInterceptorFactory,
            cacheInterceptorFactory,
            responseInterceptorFactory
    )
}

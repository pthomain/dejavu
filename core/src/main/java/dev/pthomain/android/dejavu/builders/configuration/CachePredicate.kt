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

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.STALE_ACCEPTED_FIRST
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache

sealed class CachePredicate(
        private val operation: Remote?
) : (RequestMetadata<*>) -> Remote? {

    override fun invoke(requestMetadata: RequestMetadata<*>) = operation

    object Inactive : CachePredicate(null)
    object CacheNone : CachePredicate(DoNotCache)
    object CacheAll : CachePredicate(Cache(STALE_ACCEPTED_FIRST, DEFAULT_CACHE_DURATION_IN_SECONDS))

    companion object {
        const val DEFAULT_CACHE_DURATION_IN_SECONDS = 3600 //1h
    }
}

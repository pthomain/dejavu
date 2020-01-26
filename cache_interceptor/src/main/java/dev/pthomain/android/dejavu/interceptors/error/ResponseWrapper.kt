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

package dev.pthomain.android.dejavu.interceptors.error

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.ResponseMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate

/**
 * Wraps the call and associated metadata for internal use.
 *
 * @param responseClass the target response class
 * @param response the call's response if available
 * @param metadata the call's metadata
 */
data class ResponseWrapper<O : Operation, T : RequestToken<O>, E> internal constructor(
        val responseClass: Class<*>,
        val response: Any?,
        override var metadata: ResponseMetadata<O, T, E>
) : ResponseMetadata.Holder<O, T, E>
        where E : Exception,
              E : NetworkErrorPredicate

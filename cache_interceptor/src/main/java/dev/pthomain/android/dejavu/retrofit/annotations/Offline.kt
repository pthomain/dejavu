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

package dev.pthomain.android.dejavu.retrofit.annotations

import dev.pthomain.android.dejavu.retrofit.annotations.OptionalBoolean.DEFAULT
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Retrofit annotation for calls made with an associated OFFLINE directive.
 * @see dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Offline
 *
 * @param freshOnly whether this call should only return FRESH data (either via the network or cached)
 * @see dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheStatus.isFresh
 *
 * @param mergeOnNextOnError allows exceptions to be intercepted and treated as an empty response metadata and delivered as such via onNext. Only used if the the response implements CacheMetadata.Holder. An exception is thrown otherwise.
 */
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class Offline(val freshOnly: Boolean = false,
                         val mergeOnNextOnError: OptionalBoolean = DEFAULT)

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

package dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token

import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.RequestMetadata
import java.util.*

/**
 * Represent the cache settings of the request and response.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param isCompressed whether or not the response was cached compressed
 * @param isEncrypted whether or not the response was cached encrypted
 * @param requestMetadata the hashed request metadata to be used for the unique cache key
 * @param fetchDate the optional date at which the request was made
 * @param cacheDate the optional date at which the response was cached
 * @param expiryDate the optional date at which the response will expire
 */
data class CacheToken internal constructor(val instruction: CacheInstruction,
                                           val status: CacheStatus,
                                           val isCompressed: Boolean,
                                           val isEncrypted: Boolean,
                                           val requestMetadata: RequestMetadata.Hashed,
                                           val fetchDate: Date? = null,
                                           val cacheDate: Date? = null,
                                           val expiryDate: Date? = null)

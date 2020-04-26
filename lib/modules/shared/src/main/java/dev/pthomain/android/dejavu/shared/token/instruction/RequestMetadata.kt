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

package dev.pthomain.android.dejavu.shared.token.instruction

/**
 * Holds metadata related to the request
 *
 * @param responseClass the response model's class
 * @param url the full URL of the request including query parameters
 * @param requestBody the optional body of the request
 */
sealed class RequestMetadata<R>(
        open val responseClass: Class<R>,
        open val url: String,
        open val requestBody: String? = null
)

/**
 * Holds metadata related to the request prior to internal hashing of the URL
 * and response class.
 *
 * @param responseClass the response model's class
 * @param url the full URL of the request including query parameters
 * @param requestBody the optional body of the request
 */
data class PlainRequestMetadata<R>(
        override val responseClass: Class<R>,
        override val url: String,
        override val requestBody: String? = null
) : RequestMetadata<R>(
        responseClass,
        url,
        requestBody
)

/**
 * Holds metadata related to the request after internal hashing of the URL
 * and response class.
 *
 * @param responseClass the response model's class
 * @param url the full URL of the request including query parameters
 * @param requestBody the optional body of the request
 * @param requestHash the hash of the URL and its alphabetically sorted query parameters
 * @param classHash the response class' hash
 */
sealed class HashedRequestMetadata<R : Any>(
        responseClass: Class<R>,
        url: String,
        requestBody: String?,
        open val requestHash: String,
        open val classHash: String
) : RequestMetadata<R>(
        responseClass,
        url,
        requestBody
)

data class ValidRequestMetadata<R : Any>(
        override val responseClass: Class<R>,
        override val url: String,
        override val requestBody: String?,
        override val requestHash: String,
        override val classHash: String
) : HashedRequestMetadata<R>(
        responseClass,
        url,
        requestBody,
        requestHash,
        classHash
)

data class InvalidRequestMetadata<R : Any>(
        override val responseClass: Class<R>
) : HashedRequestMetadata<R>(
        responseClass,
        DEFAULT_URL,
        null,
        INVALID_HASH,
        INVALID_HASH
)

internal const val DEFAULT_URL = "http://127.0.0.1"
internal const val INVALID_HASH = "no_hash"

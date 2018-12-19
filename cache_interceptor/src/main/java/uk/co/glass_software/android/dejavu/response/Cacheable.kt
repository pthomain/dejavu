/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.response

import uk.co.glass_software.android.dejavu.interceptors.internal.error.ApiError

/**
 * Default implementation of CacheMetadata.Holder. Have the response extend this class
 * if you want to inherit from the default metadata holding and error handling mechanisms.
 * Alternatively, if your response class cannot extend this class, have it implement the
 * CacheMetadata.Holder interface in a similar fashion as this class' implementation.
 * To provide your own error handling via an error factory, see ApiErrorFactory.
 *
 * @see uk.co.glass_software.android.dejavu.interceptors.internal.error.ApiErrorFactory
 */
abstract class Cacheable : CacheMetadata.Holder<ApiError> {

    override lateinit var metadata: CacheMetadata<ApiError>

}
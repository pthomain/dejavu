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

package dev.pthomain.android.dejavu.interceptors

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import kotlinx.coroutines.flow.Flow

// TODO: This test needs to be rewritten for Flow-based API after Phase 3 (DI migration).
// The test previously relied on RxJava Observable/Single/TestObserver types and types from
// the old Glitchy library (ResponseWrapper, ErrorInterceptor, RxType) which have been
// refactored in Phase 2. DejaVuInterceptor now uses Flow instead of ObservableTransformer.
class DejaVuInterceptorUnitTest {
    // Placeholder - tests need rewriting for coroutines/Flow API using runTest and Turbine
}

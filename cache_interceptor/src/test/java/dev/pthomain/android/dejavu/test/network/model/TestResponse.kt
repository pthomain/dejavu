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

package dev.pthomain.android.dejavu.test.network.model

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.response.HasResponseMetadata

class TestResponse : ArrayList<User>(), HasResponseMetadata<TestResponse, Cache> {

    override lateinit var cacheToken: ResponseToken<Cache, TestResponse>

    override lateinit var callDuration: CallDuration

    companion object {
        var STUB_FILE = "api_stub_test.json"
        var URL = "http://test.com"
    }

}

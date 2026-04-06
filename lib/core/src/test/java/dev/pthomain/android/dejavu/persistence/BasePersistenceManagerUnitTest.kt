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

package dev.pthomain.android.dejavu.persistence

// TODO: This test base class needs to be rewritten for the current PersistenceManager API.
// The old test referenced ResponseWrapper, ResponseMetadata, DejaVu.Configuration,
// InstructionToken and other types that have been replaced in v3.
// The PersistenceManager now works with Response<R, Cache> and Persisted.Deserialised<R>.
internal abstract class BasePersistenceManagerUnitTest {
    // Placeholder - persistence manager tests need rewriting for updated types
}

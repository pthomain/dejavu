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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.DatabasePersistenceManager

/**
 * Holder wrapping the available factory implementations of PersistenceManager
 *
 * @param filePersistenceManagerFactory a factory returning FilePersistenceManager, useful for a small number of entries
 * @param databasePersistenceManagerFactory an optional factory returning DatabasePersistenceManager, useful for a larger number of entries
 * @see dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.useDatabase
 * @param memoryPersistenceManagerFactory  a factory returning MemoryPersistenceManager, useful only if there is a strict requirement not to persist data to disk
 */
class PersistenceManagerFactory<E> internal constructor(
        val filePersistenceManagerFactory: KeyValuePersistenceManager.FileFactory<E>,
        val databasePersistenceManagerFactory: DatabasePersistenceManager.Factory<E>?,
        val memoryPersistenceManagerFactory: KeyValuePersistenceManager.MemoryFactory<E>
) where E : Exception,
        E : NetworkErrorPredicate
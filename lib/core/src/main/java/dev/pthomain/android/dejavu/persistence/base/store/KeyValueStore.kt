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

package dev.pthomain.android.dejavu.persistence.base.store


interface KeyValueStore<K, P, V> {

    /**
     * Returns an existing entry key matching the given partial key
     *
     * @param partialKey the partial key used to retrieve the full key
     * @return the matching full key if present
     */
    fun findPartialKey(partialKey: P): K?

    /**
     * Returns an entry for the given key, if present
     *
     * @param key the entry's key
     * @return the matching entry if present
     */
    fun get(key: K): V?

    /**
     * Saves an entry with a given key
     *
     * @param key the key to save the entry under
     * @param value the value to associate with the key
     */
    fun save(key: K,
             value: V)

    /**
     * @return a map of the existing entries
     */
    fun values(): Map<K, V>

    /**
     * Deletes the entry matching the given key
     *
     * @param key the entry's key
     */
    fun delete(key: K)

    /**
     * Renames an entry
     *
     * @param oldKey the old entry key
     * @param newKey  the new entry key
     */
    fun rename(oldKey: K, newKey: K)

}
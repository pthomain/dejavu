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

package dev.pthomain.android.dejavu.persistence.sqlite.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pthomain.android.dejavu.persistence.sqlite.entity.CacheEntry

@Dao
interface CacheDao {

    @Query("SELECT * FROM dejavu_cache WHERE requestHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CacheEntry)

    @Query("DELETE FROM dejavu_cache WHERE requestHash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("DELETE FROM dejavu_cache WHERE classHash = :classHash")
    suspend fun deleteByClass(classHash: String)

    @Query("DELETE FROM dejavu_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM dejavu_cache WHERE expiryDate < :now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT * FROM dejavu_cache")
    suspend fun getAll(): List<CacheEntry>

    @Query("SELECT COUNT(*) FROM dejavu_cache")
    suspend fun count(): Int

    @Query("UPDATE dejavu_cache SET expiryDate = :expiryDate WHERE requestHash = :hash")
    suspend fun updateExpiryDate(hash: String, expiryDate: Long): Int
}

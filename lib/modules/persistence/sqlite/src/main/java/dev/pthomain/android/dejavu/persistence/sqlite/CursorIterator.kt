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

package dev.pthomain.android.dejavu.persistence.sqlite

import android.database.Cursor

/**
 * This class wraps the Cursor in an Iterable interface.
 *
 * @param cursor the cursor to iterate over
 */
class CursorIterator internal constructor(
        private val cursor: Cursor
) : Iterator<Cursor>, Iterable<Cursor> {
    override fun iterator() = this
    override fun next() = cursor
    override fun hasNext() = try {
        cursor.moveToNext()
    } catch (e: Exception) {
        false
    }
}
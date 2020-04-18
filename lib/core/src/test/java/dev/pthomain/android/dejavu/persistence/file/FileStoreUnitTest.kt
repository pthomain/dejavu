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

package dev.pthomain.android.dejavu.persistence.file

import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.error.glitch.Glitch
import dev.pthomain.android.dejavu.persistence.base.BaseKeyValueStoreUnitTest
import dev.pthomain.android.dejavu.serialisation.FileNameSerialiser
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class FileStoreUnitTest : BaseKeyValueStoreUnitTest<dev.pthomain.android.dejavu.persistence.file.FileStore>() {

    private lateinit var mockCacheDirectory: File
    private lateinit var mockFileFactory: (File, String) -> File
    private lateinit var mockFileOfRightType: File
    private lateinit var mockFileOfWrongType1: File
    private lateinit var mockFileOfWrongType2: File
    private lateinit var mockValidFile: File
    private lateinit var mockFileToDelete: File
    private lateinit var mockInvalidatedFile: File
    private lateinit var mockFileInputStreamFactory: (File) -> InputStream
    private lateinit var mockFileOutputStreamFactory: (File) -> OutputStream
    private lateinit var mockFileReader: (InputStream) -> ByteArray
    private lateinit var mockOutputStream: OutputStream
    private lateinit var mockInputStream: InputStream
    private lateinit var mockConfiguration: DejaVu.Configuration<Glitch>
    private lateinit var mockFileNameSerialiser: FileNameSerialiser

    override fun setUpTarget(): dev.pthomain.android.dejavu.persistence.file.FileStore {
        mockCacheDirectory = mock()
        mockFileFactory = mock()
        mockFileOfRightType = mock()
        mockFileOfWrongType1 = mock()
        mockFileOfWrongType2 = mock()
        mockFileInputStreamFactory = mock()
        mockFileOutputStreamFactory = mock()
        mockFileReader = mock()
        mockInputStream = mock()
        mockOutputStream = mock()
        mockValidFile = mock()
        mockInvalidatedFile = mock()
        mockFileToDelete = mock()

        mockConfiguration = mock()
        mockFileNameSerialiser = mock()

        return dev.pthomain.android.dejavu.persistence.file.FileStore.Factory(
                mock(),
                mockConfiguration,
                mockFileNameSerialiser
        ).create(mockCacheDirectory)
    }


    //TODO + integration
}



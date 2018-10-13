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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache;


import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import uk.co.glass_software.android.cache_interceptor.base.BaseIntegrationTest;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SerialisationManagerIntegrationTest extends BaseIntegrationTest {
    
    private TestResponse stubbedResponse;
    
    private SerialisationManager target;
    
    @Before
    public void setUp() throws Exception {
        stubbedResponse = assetHelper.getStubbedResponse(TestResponse.STUB_FILE, TestResponse.class)
                                     .blockingFirst();
        
        target = new SerialisationManager(
                mock(Logger.class),
                StoreEntryFactory.buildDefault(RuntimeEnvironment.application),
                false,
                true,
                new Gson()
        );
    }
    
    @Test
    public void testCompress() throws Exception {
        byte[] compressed = target.serialise(stubbedResponse);
        assertEquals("Wrong compressed size", 2566, compressed.length);
    }
    
    @Test
    public void testUncompressSuccess() throws Exception {
        byte[] compressed = target.serialise(stubbedResponse);
        Action mockAction = mock(Action.class);
        
        TestResponse uncompressed = target.deserialise(TestResponse.class, compressed, mockAction);
        
        stubbedResponse.setMetadata(null); //no need to test metadata in this test
        assertEquals("Responses didn't match", stubbedResponse, uncompressed);
        verify(mockAction, never()).act();
    }
    
    @Test
    public void testUncompressFailure() throws Exception {
        byte[] compressed = target.serialise(stubbedResponse);
        for (int i = 0; i < 50; i++) {
            compressed[i] = 0;
        }
        Action mockAction = mock(Action.class);
        
        target.deserialise(TestResponse.class, compressed, mockAction);
        
        verify(mockAction).act();
    }
}
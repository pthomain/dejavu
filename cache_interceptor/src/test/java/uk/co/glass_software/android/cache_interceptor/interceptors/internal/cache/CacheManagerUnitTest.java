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

import java.util.Date;

import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.CACHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.STALE;

@SuppressWarnings("unchecked")
public class CacheManagerUnitTest {
    
    private Date mockDate;
    private CacheToken mockToken;
    private Function mockDateFactory;
    
    private CacheManager target;
    
    @Before
    public void setUp() throws Exception {
        mockDate = mock(Date.class);
        mockToken = mock(CacheToken.class);
        
        mockDateFactory = mock(Function.class);
        target = new CacheManager(
                mock(DatabaseManager.class),
                mockDateFactory,
                new Gson(),
                mock(Logger.class)
        );
    }
    
    @Test
    public void testGetCachedStatus() throws Exception {
        when(mockToken.getExpiryDate()).thenReturn(null);
        when(mockToken.getStatus()).thenReturn(CACHED);
        
        assertEquals(STALE, target.getCachedStatus(mockToken));
        
        resetMocks();
        
        when(mockDate.getTime()).thenReturn(0L);
        when(mockToken.getExpiryDate()).thenReturn(new Date(1L));
        assertEquals(CACHED, target.getCachedStatus(mockToken));
        
        resetMocks();
        
        when(mockDate.getTime()).thenReturn(1L);
        when(mockToken.getExpiryDate()).thenReturn(new Date(0L));
        assertEquals(STALE, target.getCachedStatus(mockToken));
    }
    
    private void resetMocks() {
        reset(mockToken, mockDate, mockDateFactory);
        when(mockDateFactory.get(isNull())).thenReturn(mockDate);
    }
}
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.util.Date;

import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class CacheInterceptorBuilderHelper {
    
    public final Gson gson;
    public final SerialisationManager serialisationManager;
    public final SQLiteDatabase database;
    public final Function<Long, Date> dateFactory;
    public final DatabaseManager databaseManager;
    public final CacheManager cacheManager;
    
    public CacheInterceptorBuilderHelper(Context context) {
        CacheInterceptorBuilder.Holder holder = new CacheInterceptorBuilder.Holder();
        new CacheInterceptorBuilder<ApiError>().build(
                context.getApplicationContext(),
                true,
                false,
                holder
        );
        
        gson = holder.getGson();
        serialisationManager = holder.getSerialisationManager();
        database = holder.getDatabase();
        dateFactory = holder.getDateFactory();
        databaseManager = holder.getDatabaseManager();
        cacheManager = holder.getCacheManager();
    }
    
}
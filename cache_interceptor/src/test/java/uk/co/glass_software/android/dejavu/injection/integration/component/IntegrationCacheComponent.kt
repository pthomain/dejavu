/*
 * Copyright () 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation () under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (); you may not use this file except in compliance
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

package uk.co.glass_software.android.dejavu.injection.integration.component

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Component
import io.reactivex.subjects.PublishSubject
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.dejavu.injection.component.CacheComponent
import uk.co.glass_software.android.dejavu.injection.integration.module.IntegrationCacheModule
import uk.co.glass_software.android.dejavu.injection.module.CacheModule
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.GsonSerialiser
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.retrofit.ProcessingErrorAdapter
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import java.util.*
import javax.inject.Singleton

@Singleton
@Component(modules = [IntegrationCacheModule::class])
internal interface IntegrationCacheComponent : CacheComponent<Glitch> {

    fun dateFactory(): CacheModule.Function1<Long?, Date>

    fun gsonSerialiser(): GsonSerialiser

    fun storeEntryFactory(): StoreEntryFactory

    fun encryptionManager(): EncryptionManager?

    fun serialisationManager(): SerialisationManager<Glitch>

    fun sqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback

    fun sqlOpenHelper(): SupportSQLiteOpenHelper

    fun database(): SupportSQLiteDatabase

    fun hasher(): Hasher

    fun databaseManager(): DatabaseManager<Glitch>

    fun cacheManager(): CacheManager<Glitch>

    fun errorInterceptorFactory(): CacheModule.Function2<CacheToken, Long, ErrorInterceptor<Glitch>>

    fun cacheInterceptorFactory(): CacheModule.Function2<CacheToken, Long, CacheInterceptor<Glitch>>

    fun responseInterceptor(): CacheModule.Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<Glitch>>

    fun defaultAdapterFactory(): RxJava2CallAdapterFactory

    fun processingErrorAdapterFactory(): ProcessingErrorAdapter.Factory<Glitch>

    fun cacheMetadataSubject(): PublishSubject<CacheMetadata<Glitch>>

    fun annotationProcessor(): AnnotationProcessor<Glitch>

    fun emptyResponseFactory(): EmptyResponseFactory<Glitch>

    fun supportSQLiteOpenHelper(): SupportSQLiteOpenHelper

}

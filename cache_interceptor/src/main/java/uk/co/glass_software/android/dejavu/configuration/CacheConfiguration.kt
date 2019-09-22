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

package uk.co.glass_software.android.dejavu.configuration

import android.content.Context
import android.os.Looper
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.DejaVu
import uk.co.glass_software.android.dejavu.injection.component.CacheComponent
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import uk.co.glass_software.android.mumbo.Mumbo
import uk.co.glass_software.android.mumbo.base.EncryptionManager
import java.io.File

/**
 * Class holding the global cache configuration. Values defined here are used by default
 * but (for some of them) can be overridden on a per-call basis via the use of arguments
 * passed to the call's CacheInstruction (and by extension in the Retrofit call's annotation).
 *
 * @see CacheInstruction
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.Cache
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.Clear
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.ClearAll
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.DoNotCache
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.Invalidate
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.Offline
 * @see uk.co.glass_software.android.dejavu.retrofit.annotations.Refresh
 */
data class CacheConfiguration<E> internal constructor(val context: Context,
                                                      val logger: Logger,
                                                      val errorFactory: ErrorFactory<E>,
                                                      val serialiser: Serialiser,
                                                      val encryptionManager: EncryptionManager,
                                                      val persistenceManagerPicker: ((CacheConfiguration<E>) -> PersistenceManager<E>)?,
                                                      val cacheDirectory: File?,
                                                      val isCacheEnabled: Boolean,
                                                      val encrypt: Boolean,
                                                      val compress: Boolean,
                                                      val mergeOnNextOnError: Boolean,
                                                      val allowNonFinalForSingle: Boolean,
                                                      val requestTimeOutInSeconds: Int,
                                                      val connectivityTimeoutInMillis: Long,
                                                      val cacheDurationInMillis: Long,
                                                      val cachePredicate: (responseClass: Class<*>, metadata: RequestMetadata) -> Boolean)
        where E : Exception,
              E : NetworkErrorProvider {

    companion object {

        internal fun <E> builder(errorFactory: ErrorFactory<E>,
                                 componentProvider: (CacheConfiguration<E>) -> CacheComponent<E>)
                where E : Exception,
                      E : NetworkErrorProvider =
                Builder(errorFactory, componentProvider)

    }

    class Builder<E> internal constructor(
            private val errorFactory: ErrorFactory<E>,
            private val componentProvider: (CacheConfiguration<E>) -> CacheComponent<E>
    ) where E : Exception,
            E : NetworkErrorProvider {

        private var logger: Logger? = null
        private var customErrorFactory: ErrorFactory<E>? = null
        private var mumboPicker: ((Mumbo) -> EncryptionManager)? = null

        private var persistenceManagerPicker: ((CacheConfiguration<E>) -> PersistenceManager<E>)? = null
        private var cacheDirectory: File? = null

        private var requestTimeOutInSeconds: Int = 15
        private var connectivityTimeoutInMillis: Long = 0L
        private var cacheDurationInMillis: Long = 60 * 60 * 1000 //1h

        private var isCacheEnabled = true
        private var compressData: Boolean = false
        private var encryptData: Boolean = false
        private var mergeOnNextOnError: Boolean = false
        private var allowNonFinalForSingle: Boolean = false
        private var cachePredicate: (Class<*>, RequestMetadata) -> Boolean = { _, _ -> false }

        /**
         * Disables log output (default log output is only enabled in DEBUG mode).
         */
        fun noLog() = logger(getSilentLogger())

        private fun getSilentLogger(): Logger {
            return object : Logger {
                override fun d(tagOrCaller: Any, message: String) = Unit
                override fun e(tagOrCaller: Any, message: String) = Unit
                override fun e(tagOrCaller: Any, t: Throwable, message: String?) = Unit
            }
        }

        /**
         * Sets custom logger.
         */
        fun logger(logger: Logger) = apply { this.logger = logger }

        /**
         * Sets a custom ErrorFactory implementation.
         */
        fun errorFactory(errorFactory: ErrorFactory<E>) = apply { this.customErrorFactory = errorFactory }

        /**
         * Sets network call timeout in seconds globally (default is 15s).
         */
        fun requestTimeOutInSeconds(requestTimeOutInSeconds: Int) = apply { this.requestTimeOutInSeconds = requestTimeOutInSeconds }

        /**
         *  Sets the maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
         */
        fun connectivityTimeoutInMillis(connectivityTimeoutInMillis: Long) = apply { this.connectivityTimeoutInMillis = connectivityTimeoutInMillis }

        /**
         * Sets the global cache duration in milliseconds (used by default for all calls with no specific directive,
         * see @Cache::durationInMillis for call-specific directive).
         */
        fun cacheDurationInMillis(cacheDurationInMillis: Long) = apply { this.cacheDurationInMillis = cacheDurationInMillis }

        /**
         * Enables or disables cache globally, regardless of individual call setup.
         * Error handling is still executing and errors will be delivered in 2 possible ways:
         *
         * - as metadata on the response if the response implements CacheMetadata.Holder and
         * the mergeOnNextOnError directive is set to true for the call.
         *
         * - using the default RxJava error mechanism otherwise.
         */
        fun cacheEnabled(isCacheEnabled: Boolean) = apply { this.isCacheEnabled = isCacheEnabled }

        /**
         * Sets the data compression globally (used by default for all calls with no specific directive,
         * see @Cache::compress for call-specific directive).
         */
        fun compressByDefault(compressData: Boolean) = apply { this.compressData = compressData }

        /**
         * Sets the data encryption globally (used by default for all calls with no specific directive,
         * see @Cache::encrypt for call-specific directive).
         */
        fun encryptByDefault(encryptData: Boolean = false) = apply {
            this.encryptData = encryptData
        }

        /**
         * Sets the EncryptionManager implementation. Can be used to provide a custom implementation
         * or to choose one provided by the Mumbo library. For compatibility reasons, the default is
         * Facebook Conceal, but apps targeting API 23+ should use Tink (JetPack).
         *
         * @param mumboPicker picker for the encryption implementation, with a choice of:
         * - Facebook's Conceal for API levels < 23 (see https://facebook.github.io/conceal)
         * - AndroidX's JetPack Security (Tink) implementation for API level >= 23 only (see https://developer.android.com/jetpack/androidx/releases/security)
         * - custom implementation using the EncryptionManager interface
         *
         * NB: if you are targeting API level 23 or above, you should use Tink as it is a more secure implementation.
         * However if your API level target is less than 23, using Tink will trigger a runtime exception.
         */
        fun encryption(mumboPicker: (Mumbo) -> EncryptionManager) = apply {
            this.mumboPicker = mumboPicker
        }

        private fun defaultEncryptionManager(
                mumbo: Mumbo,
                context: Context
        ) = with(mumbo) {
            context.packageManager.getApplicationInfo(
                    context.packageName,
                    0
            )?.let {
                if (it.targetSdkVersion >= 23) tink()
                else conceal()
            } ?: conceal()
        }

        /**
         * Provide a different PersistenceManager to handle the persistence of the cached requests.
         * The default implementation is DatabasePersistenceManager which saves the responses to
         * an SQLite database (using requery). This takes precedence over useFileCaching().
         *
         * @param persistenceManagerPicker a factory providing this configuration and returning the PersistenceManager implementation to use instead of the default provided one
         */
        fun persistenceManager(persistenceManagerPicker: (CacheConfiguration<E>) -> PersistenceManager<E>) = apply { this.persistenceManagerPicker = persistenceManagerPicker }

        /**
         * Uses an implementation of PersistenceManager serialising the responses to the given directory.
         * Special care needs to be taken with public directories for which encryption should be enabled
         * by default (see encryptByDefault()). This is overridden by persistenceManager().
         *
         * @param cacheDirectory the file directory to use for caching
         */
        fun useFileCaching(cacheDirectory: File) = apply { this.cacheDirectory = cacheDirectory }

        /**
         * Sets response/error merging globally (used by default for all calls with no specific directive,
         * see @Cache::mergeOnNextOnError for call-specific directive).
         *
         * When set to true, errors will be added as metadata to any call implementing
         * the CacheMetadata.Holder interface. This means onError(t:Throwable) will never be called.
         *
         * Instead if an error occurs, an empty response is returned with the exception available as
         * metadata. Special care must be taken to check if the response metadata contains an error
         * before attempting to read any of its fields.
         *
         * When used by mistake on a call returning a response that does not implement
         * CacheMetadata.Holder, this directive is ignored and the exception is delivered using the
         * default RxJava mechanism which may cause a crash if no uncaught error handler
         * is set and the onError(t:Throwable) callback is not provided.
         */
        fun mergeOnNextOnError(mergeOnNextOnError: Boolean) = apply { this.mergeOnNextOnError = mergeOnNextOnError }

        /**
         * Allows Singles to return non-final responses. This means the call terminates earlier with
         * the risk that the returned data might be STALE. The REFRESH call will still happen in
         * the background but the result of it won't be delivered. Instead it will be available
         * for the next call. By default, Singles will only return responses with final status.
         * The 'filterFinal' directive on the cache instruction will take precedence if set.
         *
         * @see uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
         * @see uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.filterFinal
         */
        fun allowNonFinalForSingle(allowNonFinalForSingle: Boolean) = apply { this.allowNonFinalForSingle = allowNonFinalForSingle }

        /**
         * Sets data caching globally (used by default for all calls with no cache instruction) using
         * the default global values set in this configuration object. The default value is to false.
         * Overrides the value set in the cachePredicate() method.
         * @see cachePredicate
         */
        fun cacheAllByDefault(cacheAllByDefault: Boolean) = apply { this.cachePredicate = { _, _ -> cacheAllByDefault } }

        /**
         * Sets a predicate for ad-hoc response caching. This predicate will be called for any
         * request that does not have an associated cache instruction. It will be called
         * with the target response class and associated request metadata before the call is made in
         * order to establish whether or not the response should be cached. Returning false means the
         * response won't be cached. Otherwise, it will be cached using the global values defined
         * in this configuration object. The default behaviour is to return false.
         * Overrides the value set in the cacheAllByDefault() method.
         * @see cacheAllByDefault
         */
        fun cachePredicate(predicate: (responseClass: Class<*>, metadata: RequestMetadata) -> Boolean) = apply { this.cachePredicate = predicate }

        /**
         * Returns an instance of DejaVu.
         *
         * @param context the Android context
         * @param serialiser custom Serialiser implementation
         */
        fun build(context: Context,
                  serialiser: Serialiser): DejaVu<E> {
            val logger = logger ?: getSilentLogger()

            RxAndroidPlugins.setInitMainThreadSchedulerHandler {
                AndroidSchedulers.from(Looper.getMainLooper(), true)
            }

            val mumbo = Mumbo(context, logger)
            val encryptionManager = mumboPicker?.invoke(mumbo)
                    ?: defaultEncryptionManager(mumbo, context)

            return DejaVu(
                    componentProvider(
                            CacheConfiguration(
                                    context.applicationContext,
                                    logger,
                                    customErrorFactory ?: errorFactory,
                                    serialiser,
                                    encryptionManager,
                                    persistenceManagerPicker,
                                    cacheDirectory,
                                    isCacheEnabled,
                                    encryptData,
                                    compressData,
                                    mergeOnNextOnError,
                                    allowNonFinalForSingle,
                                    requestTimeOutInSeconds,
                                    connectivityTimeoutInMillis,
                                    cacheDurationInMillis,
                                    cachePredicate
                            ).also { logger.d(this, "DejaVu set up with the following configuration: $it") }
                    )
            )
        }
    }

}
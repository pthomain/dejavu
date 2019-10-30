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

package dev.pthomain.android.dejavu.configuration

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.configuration.error.ErrorFactory
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CachePriority.DEFAULT
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Cache
import dev.pthomain.android.dejavu.injection.DejaVuComponent
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManagerFactory
import dev.pthomain.android.mumbo.Mumbo
import dev.pthomain.android.mumbo.base.EncryptionManager

/**
 * Class holding the global cache configuration. Values defined here are used by default
 * but (for some of them) can be overridden on a per-call basis via the use of arguments
 * passed to the call's CacheInstruction (and by extension in the Retrofit call's annotation).
 *
 * @see dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
 * @see dev.pthomain.android.dejavu.retrofit.annotations.Cache
 * @see dev.pthomain.android.dejavu.retrofit.annotations.Clear
 * @see dev.pthomain.android.dejavu.retrofit.annotations.DoNotCache
 * @see dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
 *
 * TODO update JavaDoc
 */
data class DejaVuConfiguration<E> internal constructor(val context: Context,
                                                       val logger: Logger,
                                                       val errorFactory: ErrorFactory<E>,
                                                       val serialiser: Serialiser,
                                                       val encryptionManager: EncryptionManager,
                                                       val useDatabase: Boolean,
                                                       val persistenceManagerPicker: ((PersistenceManagerFactory<E>) -> PersistenceManager<E>)?,
                                                       val isCacheEnabled: Boolean,
                                                       val requestTimeOutInSeconds: Int,
                                                       val cachePredicate: (responseClass: Class<*>, metadata: RequestMetadata) -> Cache?)
        where E : Exception,
              E : NetworkErrorPredicate {

    companion object {
        const val DEFAULT_CACHE_DURATION_IN_SECONDS = 3600 //1h
    }

    class Builder<E> internal constructor(
            private val errorFactory: ErrorFactory<E>,
            private val componentProvider: (DejaVuConfiguration<E>) -> DejaVuComponent<E>
    ) where E : Exception,
            E : NetworkErrorPredicate {

        private var logger: Logger? = null
        private var customErrorFactory: ErrorFactory<E>? = null
        private var mumboPicker: ((Mumbo) -> EncryptionManager)? = null

        private var useDatabase = false
        private var persistenceManagerPicker: ((PersistenceManagerFactory<E>) -> PersistenceManager<E>)? = null

        private var isCacheEnabled = true
        private var requestTimeOutInSeconds: Int = 15

        private var durationInSeconds = DEFAULT_CACHE_DURATION_IN_SECONDS
        private var connectivityTimeoutInSeconds: Int? = null
        private var compressData: Boolean = false
        private var encryptData: Boolean = false

        private var cachePredicate: (Class<*>, RequestMetadata) -> Cache? = { _, _ -> null }

        /**
         * Disables log output (default log output is only enabled in DEBUG mode).
         */
        fun noLog() = logger(getSilentLogger())

        private fun getSilentLogger() = object : Logger {
            override fun d(tagOrCaller: Any, message: String) = Unit
            override fun e(tagOrCaller: Any, message: String) = Unit
            override fun e(tagOrCaller: Any, t: Throwable, message: String?) = Unit
        }

        /**
         * Sets custom logger.
         */
        fun logger(logger: Logger) =
                apply { this.logger = logger }

        /**
         * Sets a custom ErrorFactory implementation.
         */
        fun errorFactory(errorFactory: ErrorFactory<E>) =
                apply { this.customErrorFactory = errorFactory }

        /**
         * Sets network call timeout in seconds globally (default is 15s).
         */
        fun requestTimeOutInSeconds(requestTimeOutInSeconds: Int) =
                apply { this.requestTimeOutInSeconds = requestTimeOutInSeconds }

        /**
         *  Sets the maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
         */
        fun connectivityTimeoutInSeconds(connectivityTimeoutInSeconds: Int) =
                apply { this.connectivityTimeoutInSeconds = connectivityTimeoutInSeconds }

        /**
         * Sets the global cache duration in milliseconds (used by default for all calls with no specific directive,
         * @see dev.pthomain.android.dejavu.retrofit.annotations.Cache.durationInSeconds for call-specific directive).
         */
        fun cacheDurationInSeconds(durationInSeconds: Int) =
                apply { this.durationInSeconds = durationInSeconds }

        /**
         * Enables or disables cache globally, regardless of individual call setup.
         * Error handling is still executing and errors will be delivered in 2 possible ways:
         *
         * - as metadata on the response if the response implements CacheMetadata.Holder and
         * the mergeOnNextOnError directive is set to true for the call.
         *
         * - using the default RxJava error mechanism otherwise.
         */
        fun cacheEnabled(isCacheEnabled: Boolean) =
                apply { this.isCacheEnabled = isCacheEnabled }

        /**
         * Sets the data compression globally (used by default for all calls with no specific directive,
         * see @Cache::compress for call-specific directive).
         */
        fun compressByDefault(compressData: Boolean) =
                apply { this.compressData = compressData }

        /**
         * Sets the data encryption globally (used by default for all calls with no specific directive,
         * see @Cache::encrypt for call-specific directive).
         */
        fun encryptByDefault(encryptData: Boolean = false) =
                apply { this.encryptData = encryptData }

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
        fun encryption(mumboPicker: (Mumbo) -> EncryptionManager) =
                apply { this.mumboPicker = mumboPicker }

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
         * @see dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
         * @see dev.pthomain.android.dejavu.interceptors.cache.persistence.database.DatabasePersistenceManager
         *
         * @param enableDatabase whether or not to instantiate the classes related to database caching (this will give access to DatabasePersistenceManager.Factory)
         * @param persistenceManagerPicker a picker providing a PersistenceManagerFactory and returning the PersistenceManager implementation to override the default one
         */
        fun persistenceManager(enableDatabase: Boolean,
                               persistenceManagerPicker: (PersistenceManagerFactory<E>) -> PersistenceManager<E>) =
                apply {
                    useDatabase = enableDatabase
                    this.persistenceManagerPicker = persistenceManagerPicker
                }

        /**
         * Sets data caching globally (used by default for all calls with no cache instruction) using
         * the default global values set in this configuration object. The default value is to false.
         * Overrides the value set in the cachePredicate() method.
         * @see cachePredicate
         */
        fun cacheAllByDefault(cacheAllByDefault: Boolean) =
                apply {
                    this.cachePredicate = { _, _ ->
                        Cache(
                                DEFAULT,
                                DEFAULT_CACHE_DURATION_IN_SECONDS,
                                connectivityTimeoutInSeconds,
                                encryptData,
                                compressData
                        )
                    }
                }

        /**
         * Sets a predicate for ad-hoc response caching. This predicate will be called for any
         * request that does not have an associated cache instruction. It will be called
         * with the target response class and associated request metadata before the call is made in
         * order to establish whether or not the response should be cached. Returning false means the
         * response won't be cached. Otherwise, it will be cached using the global values defined
         * in this configuration object. The default behaviour is to return false.
         * Overrides the value set in the cacheAllByDefault() method.
         *
         * //TODO update JavaDoc
         * @see cacheAllByDefault
         */
        fun cachePredicate(predicate: (responseClass: Class<*>, metadata: RequestMetadata) -> Cache?) =
                apply { this.cachePredicate = predicate }

        /**
         * Returns an instance of DejaVu.
         *
         * @param context the Android context
         * @param serialiser the mandatory custom Serialiser implementation
         */
        fun build(context: Context,
                  serialiser: Serialiser): DejaVu<E> {
            val logger = logger ?: getSilentLogger()

            val encryptionManager = with(Mumbo(context, logger)) {
                mumboPicker?.invoke(this)
                        ?: defaultEncryptionManager(this, context)
            }

            return DejaVu(
                    componentProvider(
                            DejaVuConfiguration(
                                    context.applicationContext,
                                    logger,
                                    customErrorFactory ?: errorFactory,
                                    serialiser,
                                    encryptionManager,
                                    useDatabase,
                                    persistenceManagerPicker,
                                    isCacheEnabled,
                                    requestTimeOutInSeconds,
                                    cachePredicate
                            ).also { logger.d(this, "DejaVu set up with the following configuration: $it") }
                    )
            )
        }
    }

}
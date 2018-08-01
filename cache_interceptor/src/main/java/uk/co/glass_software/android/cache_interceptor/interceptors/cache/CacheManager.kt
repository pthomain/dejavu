package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.support.annotation.VisibleForTesting
import com.google.gson.Gson

import java.util.Date

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javolution.util.stripped.FastMap.logger
import org.apache.commons.lang3.ClassUtils.getSimpleName
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.Cache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.Refresh
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.*
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

import uk.co.glass_software.android.shared_preferences.utils.Logger

internal class CacheManager<E>(private val databaseManager: DatabaseManager<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val gson: Gson,
                               private val logger: Logger)
        where E : Exception,
              E : (E) -> kotlin.Boolean {

    fun clearOlderEntries() {
        runAsync(databaseManager::clearOlderEntries)
    }

    fun clearCache() {
        runAsync(databaseManager::clearCache)
    }

    private fun runAsync(action: () -> Unit) {
        Completable.fromAction { action() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          cacheOperation: CacheInstruction.Operation.Expiring,
                          isNetworkError: (E) -> Boolean)
            : Observable<ResponseWrapper<E>> {

        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Checking for cached $simpleName")

        val cachedResponse = databaseManager.getCachedResponse(instructionToken)

        if (cachedResponse == null) {
            logger.d(this, "No cached $simpleName")
            return fetchAndCache(
                    upstream,
                    cacheOperation,
                    instructionToken
            )
        } else {
            val metadata = cachedResponse.metadata!!
            val cachedResponseToken = metadata.cacheToken!!
            val status = getCachedStatus(cachedResponseToken)
            cachedResponse.metadata = cachedResponse.metadata!!.copy(
                    cacheToken = cachedResponseToken.copy(status = status)
            )
            logger.d(this, "Found cached $simpleName, status: $status")

            return if (status === STALE) {
                refreshStale(
                        cachedResponse,
                        cacheOperation,
                        upstream,
                        isNetworkError
                )
            } else {
                Observable.just(cachedResponse)
            }
        }
    }

    @VisibleForTesting
    fun getCachedStatus(cacheToken: CacheToken) = cacheToken.expiryDate?.let {
        if (dateFactory(null).time > it.time) STALE else CACHED
    } ?: STALE

    private fun fetchAndCache(upstream: Observable<ResponseWrapper<E>>,
                              cacheOperation: CacheInstruction.Operation.Expiring,
                              instructionToken: CacheToken)
            : Observable<ResponseWrapper<E>> {

        val simpleName = instructionToken.instruction.responseClass.simpleName
        logger.d(this, "Fetching and caching new $simpleName")

        return upstream
                .doOnNext { responseWrapper ->
                    logger.d(this, "Finished fetching $simpleName, now delivering")
                    val metadata = responseWrapper.metadata!!

                    if (metadata.exception == null) {
                        val fetchDate = dateFactory(null)
                        val timeToLiveInMs = cacheOperation.durationInMillis
                        val expiryDate = dateFactory(fetchDate.time + timeToLiveInMs.toLong())

                        responseWrapper.metadata = responseWrapper.metadata!!.copy(
                                cacheToken = CacheToken.caching(
                                        instructionToken,
                                        fetchDate,
                                        fetchDate,
                                        expiryDate
                                )
                        )
                    }
                }
                .doAfterNext { response ->
                    if (response.metadata!!.exception == null) {
                        logger.d(this, "$simpleName successfully delivered, now caching")
                        databaseManager.cache(instructionToken, response)
                    }
                }
    }

    private fun refreshStale(cachedResponse: ResponseWrapper<E>,
                             refreshOperation: CacheInstruction.Operation.Expiring,
                             upstream: Observable<ResponseWrapper<E>>,
                             isNetworkError: (E) -> Boolean)
            : Observable<ResponseWrapper<E>> {

        val metadata = cachedResponse.metadata!!
        val cacheToken = metadata.cacheToken!!
        val simpleName = cacheToken.instruction.responseClass.simpleName

        logger.d(this, "$simpleName is ${cacheToken.status}, attempting to refresh")

        val fetchAndCache = fetchAndCache(
                upstream,
                refreshOperation,
                cacheToken
        ).map { responseWrapper ->
            val error = responseWrapper.metadata!!.exception
            val isCouldNotRefresh = error != null && isNetworkError(error)

            if (isCouldNotRefresh) {
                deepCopy(cachedResponse, COULD_NOT_REFRESH)
            } else {
                updateRefreshed(responseWrapper, REFRESHED)
            }
        }

        return Observable.just(Observable.just(cachedResponse), fetchAndCache)
                .concatMap { observable -> observable.subscribeOn(Schedulers.io()) }
                as Observable<ResponseWrapper<E>>
    }

    private fun deepCopy(cachedResponse: ResponseWrapper<E>,
                         newStatus: CacheStatus): ResponseWrapper<E> {
        val copiedResponse = gson.fromJson(
                gson.toJson(cachedResponse.response),
                cachedResponse.responseClass
        )

        return cachedResponse.copy(
                response = copiedResponse,
                metadata = updateStatus(cachedResponse, newStatus)
        )
    }

    private fun updateRefreshed(response: ResponseWrapper<E>,
                                newStatus: CacheStatus): ResponseWrapper<E> {
        val simpleName = response.metadata!!.cacheToken!!.instruction.responseClass.simpleName
        response.metadata = updateStatus(response, newStatus)

        logger.d(
                this,
                "Delivering ${if (newStatus === REFRESHED) "refreshed" else "stale"} $simpleName, status: $newStatus"
        )

        return response
    }

    private fun updateStatus(response: ResponseWrapper<E>,
                             newStatus: CacheStatus): CacheMetadata<E> {
        val metadata = response.metadata!!
        val oldToken = metadata.cacheToken!!
        val newToken = oldToken.copy(status = newStatus)

        return metadata.copy(cacheToken = newToken)
    }

}
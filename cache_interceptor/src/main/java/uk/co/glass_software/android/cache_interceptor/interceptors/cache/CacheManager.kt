package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import com.google.gson.Gson

import java.util.Date

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.Cache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.CACHED
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.STALE
import uk.co.glass_software.android.shared_preferences.utils.Logger

internal class CacheManager(private val databaseManager: DatabaseManager,
                            private val dateFactory: (Long?) -> Date,
                            private val gson: Gson,
                            private val logger: Logger) {

    fun clearOlderEntries() {
        runAsync(databaseManager::clearOlderEntries)
    }

    fun flushCache() {
        runAsync(databaseManager::flushCache)
    }

    private fun runAsync(action: () -> Unit) {
        Completable.fromAction { action() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun <E> getCachedResponse(upstream: Observable<ResponseWrapper>,
                              instruction: CacheInstruction,
                              cacheOperation: CacheInstruction.Operation.Cache,
                              url: String,
                              body: String?,
                              isNetworkError: (E) -> Boolean): Observable<ResponseWrapper> {
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Checking for cached $simpleName")

        val instructionToken = CacheToken.fromInstruction(
                instruction,
                url,
                body
        )

        val cachedResponse = databaseManager.getCachedResponse(instructionToken)

        if (cachedResponse == null) {
            logger.d(this, "No cached $simpleName")
            return fetchAndCache(upstream, cacheOperation)
        } else {
            val metadata = cachedResponse!!.getMetadata()
            val cachedResponseToken = metadata.responseToken
            val status = getCachedStatus<MetadataHolder<R>>(cachedResponseToken!!)
            metadata.responseToken = CacheToken.newStatus(cachedResponseToken, status)

            logger.d(this, "Found cached $simpleName, status: $status")

            return if (status === STALE) {
                refreshStale(
                        cachedResponse!!,
                        upstream,
                        isNetworkError
                )
            } else {
                Observable.just<T>(cachedResponse!!)
            }
        }
    }

    fun <R : MetadataHolder<R>> getCachedStatus(cacheToken: CacheToken<out R>): CacheStatus {
        val expiryDate = cacheToken.expiryDate ?: return STALE
        return if (dateFactory.get(null).getTime() > expiryDate.getTime()) STALE else CACHED
    }

    private fun fetchAndCache(upstream: Observable<ResponseWrapper>,
                              cacheOperation: CacheInstruction.Operation.Cache,
                              cacheToken: CacheToken): Observable<ResponseWrapper> {
        val simpleName = cacheToken.responseClass.simpleName
        logger.d(this, "Fetching and caching new $simpleName")

        return upstream
                .doOnNext { responseWrapper ->
                    logger.d(this, "Finished fetching $simpleName, now delivering")
                    val metadata = responseWrapper.metadata

                    if (metadata?.exception == null) {
                        val fetchDate = dateFactory(null)
                        val timeToLiveInMs = cacheOperation.durationInMillis
                        val expiryDate = dateFactory(fetchDate.time + timeToLiveInMs.toLong())
                        metadata.responseToken = CacheToken.caching(
                                cacheToken,
                                fetchDate,
                                fetchDate,
                                expiryDate
                        )
                    }
                }
                .doAfterNext { response ->
                    if (response.getMetadata().getException() == null) {
                        logger.d(this, simpleName + " successfully delivered, now caching")
                        databaseManager.cache(response)
                    }
                }
    }

    private fun refreshStale(cachedResponse: R,
                             upstream: Observable<R>,
                             isNetworkError: Function<Exception, Boolean>): Observable<R> {
        val simpleName = cachedResponse.getClass().getSimpleName()
        val metadata = cachedResponse.getMetadata()
        val cacheToken = metadata.getRequestToken()

        logger.d(this, simpleName + " is " + cacheToken.status + ", attempting to refresh")

        val fetchAndCache = fetchAndCache(upstream, cacheToken)
                .map<R> { response ->
                    val error = response.getMetadata().getException()
                    val isCouldNotRefresh = error != null && isNetworkError.get(error)

                    if (isCouldNotRefresh) {
                        return@fetchAndCache upstream, cacheToken)
                        .map deepCopy cachedResponse, CacheStatus.COULD_NOT_REFRESH)
                    } else {
                        return@fetchAndCache upstream, cacheToken)
                        .map updateRefreshed response, CacheStatus.REFRESHED)
                    }
                }

        return Observable.just(Observable.just(cachedResponse), fetchAndCache)
                .concatMap { observable -> observable.subscribeOn(Schedulers.io()) }
    }

    private fun <R : MetadataHolder<R>> deepCopy(cachedResponse: R,
                                                 newStatus: CacheStatus): R {
        val copiedResponse = gson.fromJson(gson.toJson(cachedResponse),
                cachedResponse.getClass() as Class<R>
        )
        val metadata = cachedResponse.getMetadata()
        val newToken = CacheToken.newStatus(metadata.getCacheToken(), newStatus)
        val copiedMetadata = CacheMetadata(newToken, null)
        copiedResponse.setMetadata(copiedMetadata)
        return copiedResponse
    }

    private fun <R : MetadataHolder<R>> updateRefreshed(response: R,
                                                        status: CacheStatus): R {
        val simpleName = response.getClass().getSimpleName()
        val metadata = response.getMetadata()
        val newToken = CacheToken.newStatus(metadata.getCacheToken(), status)
        metadata.setCacheToken(newToken)

        logger.d(this,
                "Delivering "
                        + (if (status === CacheStatus.REFRESHED) "refreshed" else "stale")
                        + " "
                        + simpleName
                        + ", status: "
                        + newToken.status
        )

        return response
    }

}
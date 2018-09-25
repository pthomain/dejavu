package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.On
import uk.co.glass_software.android.boilerplate.utils.rx.schedule
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.*
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*


internal class CacheManager<E>(private val databaseManager: DatabaseManager<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val gson: Gson,
                               private val logger: Logger)
        where E : Exception,
              E : (E) -> kotlin.Boolean {

    fun clearCache(typeToClear: Class<*>?, //TODO
                   clearOlderEntriesOnly: Boolean) =
            Completable.create {
                if (clearOlderEntriesOnly) {
                    databaseManager.clearOlderEntries()
                } else {
                    databaseManager.clearCache()
                }
                it.onComplete()
            }!!

    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          cacheOperation: CacheInstruction.Operation.Expiring,
                          isNetworkError: (E) -> Boolean)
            : Observable<ResponseWrapper<E>> {

        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d("Checking for cached $simpleName")

        val cachedResponse = databaseManager.getCachedResponse(instructionToken)

        if (cachedResponse == null) {
            logger.d("No cached $simpleName")
            return fetchAndCache(
                    upstream,
                    cacheOperation,
                    instructionToken
            )
        } else {
            val metadata = cachedResponse.metadata!!
            val cachedResponseToken = metadata.cacheToken
            val status = getCachedStatus(cachedResponseToken)
            cachedResponse.metadata = metadata.copy(
                    cacheToken = cachedResponseToken.copy(status = status)
            )
            logger.d("Found cached $simpleName, status: $status")

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
        logger.d("Fetching and caching new $simpleName")

        return upstream
                .doOnNext { responseWrapper ->
                    logger.d("Finished fetching $simpleName, now delivering")
                    val metadata = responseWrapper.metadata!!

                    if (metadata.exception == null) {
                        val fetchDate = dateFactory(null)
                        val timeToLiveInMs = cacheOperation.durationInMillis
                        val expiryDate = dateFactory(fetchDate.time + timeToLiveInMs.toLong())

                        responseWrapper.metadata = metadata.copy(
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
                        logger.d("$simpleName successfully delivered, now caching")
                        databaseManager.cache(
                                instructionToken,
                                cacheOperation,
                                response
                        ).subscribe({}, { logger.e(it, "Could not cache $simpleName") })
                    }
                }
    }

    private fun refreshStale(cachedResponse: ResponseWrapper<E>,
                             refreshOperation: CacheInstruction.Operation.Expiring,
                             upstream: Observable<ResponseWrapper<E>>,
                             isNetworkError: (E) -> Boolean)
            : Observable<ResponseWrapper<E>> {

        val metadata = cachedResponse.metadata!!
        val cacheToken = metadata.cacheToken
        val simpleName = cacheToken.instruction.responseClass.simpleName

        logger.d("$simpleName is ${cacheToken.status}, attempting to refresh")

        val fetchAndCache = fetchAndCache(
                upstream,
                refreshOperation,
                cacheToken
        ).map { responseWrapper ->
            val error = responseWrapper.metadata!!.exception
            val isCouldNotRefresh = error != null && isNetworkError(error)

            if (isCouldNotRefresh) {
                cachedResponse.copy(
                        response = cachedResponse.response,
                        metadata = updateStatus(cachedResponse, COULD_NOT_REFRESH)
                )
            } else {
                updateRefreshed(responseWrapper, REFRESHED)
            }
        }

        return Observable.just(Observable.just(cachedResponse), fetchAndCache)
                .concatMap { observable -> observable.schedule(On.Io, On.Trampoline) }
                as Observable<ResponseWrapper<E>>
    }

    private fun updateRefreshed(response: ResponseWrapper<E>,
                                newStatus: CacheStatus): ResponseWrapper<E> {
        val simpleName = response.metadata!!.cacheToken.instruction.responseClass.simpleName
        response.metadata = updateStatus(response, newStatus)

        logger.d("Delivering ${if (newStatus === REFRESHED) "refreshed" else "stale"} $simpleName, status: $newStatus")

        return response
    }

    private fun updateStatus(response: ResponseWrapper<E>,
                             newStatus: CacheStatus): CacheMetadata<E> {
        val metadata = response.metadata
        val oldToken = metadata!!.cacheToken
        val newToken = oldToken.copy(status = newStatus)

        return metadata.copy(cacheToken = newToken)
    }

}
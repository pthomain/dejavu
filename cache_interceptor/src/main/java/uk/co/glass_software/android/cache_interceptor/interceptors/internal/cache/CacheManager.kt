package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache

import io.reactivex.Completable
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.On
import uk.co.glass_software.android.boilerplate.utils.rx.schedule
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.*
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*


internal class CacheManager<E>(private val databaseManager: DatabaseManager<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val defaultDurationInMillis: Long,
                               private val logger: Logger)
        where E : Exception,
              E : NetworkErrorProvider {

    fun clearCache(typeToClear: Class<*>?,
                   clearOlderEntriesOnly: Boolean) = Completable.create {
        databaseManager.clearCache(typeToClear, clearOlderEntriesOnly)
        it.onComplete()
    }!!

    fun invalidate(instructionToken: CacheToken) = Completable.create {
        databaseManager.invalidate(instructionToken)
        it.onComplete()
    }!!

    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          cacheOperation: Expiring,
                          start: Long)
            : Observable<ResponseWrapper<E>> {

        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName

        val isRefreshFreshOnly = cacheOperation.freshOnly && cacheOperation is Refresh

        logger.d("Checking for cached $simpleName")
        val cachedResponse = databaseManager.getCachedResponse(instructionToken, start)

        val diskDuration = cachedResponse?.metadata?.callDuration?.disk
                ?: (System.currentTimeMillis() - start).toInt()

        if (cachedResponse == null) {
            return fetchAndCache(
                    null,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration,
                    isRefreshFreshOnly
            )
        } else {
            val metadata = cachedResponse.metadata
            val cachedResponseToken = metadata.cacheToken
            val status = cachedResponseToken.status

            logger.d("Found cached $simpleName, status: $status")

            return if (status === STALE) {
                refreshStale(
                        cachedResponse,
                        diskDuration,
                        cacheOperation,
                        upstream
                ).filter { !cacheOperation.freshOnly || it.metadata.cacheToken.status.isFresh }
            } else {
                Observable.just(cachedResponse)
            }
        }
    }

    private fun fetchAndCache(previousCachedResponse: ResponseWrapper<E>?,
                              upstream: Observable<ResponseWrapper<E>>,
                              cacheOperation: Expiring,
                              instructionToken: CacheToken,
                              diskDuration: Int,
                              isRefreshFreshOnly: Boolean)
            : Observable<ResponseWrapper<E>> {

        val simpleName = instructionToken.instruction.responseClass.simpleName
        logger.d("Fetching and caching new $simpleName")

        return upstream
                .doOnNext { responseWrapper ->
                    logger.d("Finished fetching $simpleName, now delivering")
                    val metadata = responseWrapper.metadata

                    responseWrapper.metadata = if (metadata.exception == null) {
                        val fetchDate = dateFactory(null)
                        val timeToLiveInMs = cacheOperation.durationInMillis
                                ?: defaultDurationInMillis
                        val expiryDate = dateFactory(fetchDate.time + timeToLiveInMs)

                        val (encryptData, compressData) = databaseManager.wasPreviouslyEncrypted(
                                previousCachedResponse,
                                cacheOperation
                        )

                        val cacheToken = CacheToken.caching(
                                instructionToken,
                                compressData,
                                encryptData,
                                fetchDate,
                                fetchDate,
                                expiryDate
                        )

                        metadata.copy(
                                cacheToken = if (isRefreshFreshOnly) cacheToken.copy(status = REFRESHED)
                                else cacheToken,
                                callDuration = getRefreshCallDuration(metadata.callDuration, diskDuration)
                        )
                    } else {
                        metadata.copy(
                                cacheToken = metadata.cacheToken.copy(status = EMPTY),
                                callDuration = getRefreshCallDuration(metadata.callDuration, diskDuration)
                        )
                    }
                }
                .doAfterNext { response ->
                    if (response.metadata.exception == null) {
                        logger.d("$simpleName successfully delivered, now caching")
                        databaseManager.cache(
                                instructionToken,
                                cacheOperation,
                                response,
                                previousCachedResponse
                        ).subscribe({}, { logger.e(it, "Could not cache $simpleName") })
                    }
                }
    }

    private fun getRefreshCallDuration(callDuration: CacheMetadata.Duration,
                                       diskDuration: Int) =
            callDuration.copy(
                    disk = diskDuration,
                    network = callDuration.network - diskDuration
            )

    private fun refreshStale(previousCachedResponse: ResponseWrapper<E>,
                             diskDuration: Int,
                             refreshOperation: Expiring,
                             upstream: Observable<ResponseWrapper<E>>)
            : Observable<ResponseWrapper<E>> {

        val metadata = previousCachedResponse.metadata
        val cacheToken = metadata.cacheToken
        val simpleName = cacheToken.instruction.responseClass.simpleName

        logger.d("$simpleName is ${cacheToken.status}, attempting to refresh")

        val fetchAndCache = fetchAndCache(
                previousCachedResponse,
                upstream,
                refreshOperation,
                cacheToken,
                diskDuration,
                false
        ).map { responseWrapper ->
            val error = responseWrapper.metadata.exception

            if (error != null) {
                val isCouldNotRefresh = error.isNetworkError() && !refreshOperation.freshOnly
                previousCachedResponse.copy(
                        response = if (isCouldNotRefresh) previousCachedResponse.response else null,
                        metadata = updateRefreshStatus(
                                responseWrapper.metadata,
                                error,
                                if (isCouldNotRefresh) COULD_NOT_REFRESH else EMPTY
                        )
                )
            } else {
                updateRefreshed(responseWrapper, REFRESHED)
            }
        }

        return Observable.just(Observable.just(previousCachedResponse), fetchAndCache)
                .concatMap { observable -> observable.schedule(On.Io, On.Trampoline) }
                as Observable<ResponseWrapper<E>>
    }

    private fun updateRefreshed(response: ResponseWrapper<E>,
                                newStatus: CacheStatus): ResponseWrapper<E> {
        val simpleName = response.metadata.cacheToken.instruction.responseClass.simpleName
        response.metadata = updateRefreshStatus(
                response.metadata,
                null,
                newStatus
        )

        logger.d("Delivering $simpleName, status: $newStatus")

        return response
    }

    private fun updateRefreshStatus(metadata: CacheMetadata<E>,
                                    exception: E?,
                                    newStatus: CacheStatus): CacheMetadata<E> {
        val oldToken = metadata.cacheToken
        val newToken = oldToken.copy(status = newStatus)

        return metadata.copy(
                cacheToken = newToken,
                exception = exception
        )
    }

}
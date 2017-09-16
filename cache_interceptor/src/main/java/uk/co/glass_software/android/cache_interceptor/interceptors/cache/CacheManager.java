package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Date;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.CACHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.STALE;

class CacheManager {
    
    private final Function<Long, Date> dateFactory;
    private final Gson gson;
    private final Logger logger;
    private final long timeToLiveInMs;
    private final DatabaseManager databaseManager;
    
    CacheManager(DatabaseManager databaseManager,
                 Function<Long, Date> dateFactory,
                 Gson gson,
                 Logger logger,
                 int timeToLiveInMinutes) {
        this.databaseManager = databaseManager;
        this.dateFactory = dateFactory;
        this.gson = gson;
        this.logger = logger;
        this.timeToLiveInMs = timeToLiveInMinutes * 60000L;
    }
    
    public void clearOlderEntries() {
        //FIXME
//        runAsync(databaseManager::clearOlderEntries);
    }
    
    public void flushCache() {
        runAsync(databaseManager::flushCache);
    }
    
    private void runAsync(Action action) {
        Completable.fromAction(action::act)
                   .subscribeOn(Schedulers.io())
                   .subscribe();
    }
    
    @SuppressWarnings("unchecked")
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> Observable<R> getCachedResponse(
            Observable<R> upstream,
            @NonNull CacheToken<R> cacheToken) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Checking for cached " + simpleName);
        
        R cachedResponse = databaseManager.getCachedResponse(upstream, cacheToken);
        
        if (cachedResponse == null) {
            logger.d(this, "No cached " + simpleName);
            return fetchAndCache(upstream, cacheToken);
        }
        else {
            ResponseMetadata<R, E> metadata = cachedResponse.getMetadata();
            CacheToken<R> cachedResponseToken = metadata.getCacheToken();
            CacheToken.Status status = getCachedStatus(cachedResponseToken);
            metadata.setCacheToken(CacheToken.newStatus(cachedResponseToken, status));
            
            logger.d(this, "Found cached " + simpleName + ", status: " + status);
            
            if (status == STALE) {
                return refreshStale(cachedResponse, upstream);
            }
            else {
                return Observable.just(cachedResponse);
            }
        }
    }
    
    @NonNull
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> CacheToken.Status getCachedStatus(
            @NonNull CacheToken<? extends R> cacheToken) {
        Date expiryDate = cacheToken.getExpiryDate();
        if (expiryDate == null) {
            return STALE;
        }
        return dateFactory.get(null).getTime() > expiryDate.getTime() ? STALE : CACHED;
    }
    
    private <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> Observable<R> fetchAndCache(
            Observable<R> upstream,
            CacheToken<R> cacheToken) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Fetching and caching new " + simpleName);
        
        return upstream
                .doOnNext(response -> {
                    logger.d(this, "Finished fetching " + simpleName + ", now delivering");
                    ResponseMetadata<R, E> metadata = response.getMetadata();
                    if (metadata.getError() == null) {
                        Date fetchDate = dateFactory.get(null);
                        Date expiryDate = dateFactory.get(fetchDate.getTime() + timeToLiveInMs);
                        metadata.setCacheToken(CacheToken.caching(cacheToken,
                                                                  upstream,
                                                                  fetchDate,
                                                                  fetchDate,
                                                                  expiryDate
                        ));
                    }
                })
                .doAfterNext(response -> {
                    if (response.getMetadata().getError() == null) {
                        logger.d(this, simpleName + " successfully delivered, now caching");
                        databaseManager.cache(response);
                    }
                });
    }
    
    @SuppressWarnings("unchecked")
    private <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> Observable<R> refreshStale(
            R cachedResponse,
            Observable<R> upstream) {
        String simpleName = cachedResponse.getClass().getSimpleName();
        ResponseMetadata<R, E> metadata = cachedResponse.getMetadata();
        CacheToken<R> cacheToken = metadata.getCacheToken();
        
        logger.d(this, simpleName + " is " + cacheToken.getStatus() + ", attempting to refresh");
        
        Observable<R> fetchAndCache = fetchAndCache(upstream, cacheToken)
                .map(response -> {
                    E error = response.getMetadata().getError();
                    
                    boolean isCouldNotRefresh = error != null
                                                && response.getMetadata().getError().get(error);
                    
                    if (isCouldNotRefresh) {
                        return deepCopy(cachedResponse, CacheToken.Status.COULD_NOT_REFRESH);
                    }
                    else {
                        return updateRefreshed(response, CacheToken.Status.REFRESHED);
                    }
                });
        
        return Observable.just(Observable.just(cachedResponse), fetchAndCache)
                         .concatMap(observable -> observable.subscribeOn(Schedulers.io()));
    }
    
    @SuppressWarnings("unchecked")
    private <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R deepCopy(
            R cachedResponse,
            CacheToken.Status newStatus) {
        R copiedResponse = gson.fromJson(gson.toJson(cachedResponse),
                                         (Class<R>) cachedResponse.getClass()
        );
        ResponseMetadata<R, E> metadata = cachedResponse.getMetadata();
        CacheToken newToken = CacheToken.newStatus(metadata.getCacheToken(), newStatus);
        metadata.setCacheToken(newToken);
        return copiedResponse;
    }
    
    private <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R updateRefreshed(
            R response,
            CacheToken.Status status) {
        String simpleName = response.getClass().getSimpleName();
        ResponseMetadata<R, E> metadata = response.getMetadata();
        CacheToken<R> newToken = CacheToken.newStatus(metadata.getCacheToken(), status);
        metadata.setCacheToken(newToken);
        
        logger.d(this,
                 "Delivering "
                 + (status == CacheToken.Status.REFRESHED ? "refreshed" : "stale")
                 + " "
                 + simpleName
                 + ", status: "
                 + newToken.getStatus()
        );
        
        return response;
    }
}
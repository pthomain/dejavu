package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

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
                 long timeToLiveInMinutes) {
        this.databaseManager = databaseManager;
        this.dateFactory = dateFactory;
        this.gson = gson;
        this.logger = logger;
        this.timeToLiveInMs = timeToLiveInMinutes * 60000L;
    }
    
    public void flushCache() {
        databaseManager.flushCache();
    }
    
    @SuppressWarnings("unchecked")
    public <E, R extends CacheToken.Holder<R, E>> Observable<R> getCachedResponse(Observable<R> upstream,
                                                                                  @NonNull CacheToken<R> cacheToken,
                                                                                  Function<E, Boolean> isNetworkError) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Checking for cached " + simpleName);
        
        R cachedResponse = databaseManager.getCachedResponse(upstream, cacheToken);
        
        if (cachedResponse == null) {
            logger.d(this, "No cached " + simpleName);
            return fetchAndCache(upstream, cacheToken);
        }
        else {
            CacheToken<R> cachedResponseToken = cachedResponse.getCacheToken();
            CacheToken.Status status = getCachedStatus(cachedResponseToken);
            cachedResponse.setCacheToken(CacheToken.newStatus(cachedResponseToken, status));
            
            logger.d(this, "Found cached " + simpleName + ", status: " + status);
            
            if (status == CacheToken.Status.STALE) {
                return refreshStale(cachedResponse,
                                    upstream,
                                    isNetworkError
                );
            }
            else {
                return Observable.just(cachedResponse);
            }
        }
    }
    
    @NonNull
    <E, R extends CacheToken.Holder<R, E>> CacheToken.Status getCachedStatus(@NonNull CacheToken<R> cacheToken) {
        Date expiryDate = cacheToken.getExpiryDate();
        if (expiryDate == null) {
            return CacheToken.Status.STALE;
        }
        return dateFactory.get(null).getTime() > expiryDate.getTime() ? CacheToken.Status.STALE : CacheToken.Status.CACHED;
    }
    
    private <E, R extends CacheToken.Holder<R, E>> Observable<R> fetchAndCache(Observable<R> upstream,
                                                                               CacheToken<R> cacheToken) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Fetching and caching new " + simpleName);
        
        return upstream
                .doOnNext(response -> {
                    logger.d(this, "Finished fetching" + simpleName + ", now delivering");
                    if (response.getError() == null) {
                        Date fetchDate = dateFactory.get(null);
                        Date expiryDate = dateFactory.get(fetchDate.getTime() + timeToLiveInMs);
                        response.setCacheToken(CacheToken.caching(cacheToken,
                                                                  upstream,
                                                                  fetchDate,
                                                                  fetchDate,
                                                                  expiryDate
                        ));
                    }
                })
                .doAfterNext(response -> {
                    if (response.getError() == null) {
                        logger.d(this, simpleName + " successfully delivered, now caching");
                        databaseManager.cache(response);
                    }
                });
    }
    
    @SuppressWarnings("unchecked")
    private <E, R extends CacheToken.Holder<R, E>> Observable<R> refreshStale(R cachedResponse,
                                                                              Observable<R> upstream,
                                                                              Function<E, Boolean> isNetworkError) {
        String simpleName = cachedResponse.getClass().getSimpleName();
        logger.d(this, simpleName + " is " + cachedResponse.getCacheToken().getStatus() + ", attempting to refresh");
        CacheToken<R> cacheToken = cachedResponse.getCacheToken();
        
        Observable<R> fetchAndCache = fetchAndCache(upstream, cacheToken)
                .map(response -> {
                    E error = response.getError();
                    boolean isCouldNotRefresh = error != null && isNetworkError.get(error);
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
    private <E, R extends CacheToken.Holder<R, E>> R deepCopy(R cachedResponse,
                                                              CacheToken.Status newStatus) {
        R rp = gson.fromJson(gson.toJson(cachedResponse), (Class<R>) cachedResponse.getClass());
        CacheToken newToken = CacheToken.newStatus(cachedResponse.getCacheToken(), newStatus);
        rp.setCacheToken(newToken);
        return rp;
    }
    
    private <E, R extends CacheToken.Holder<R, E>> R updateRefreshed(R response,
                                                                     CacheToken.Status status) {
        String simpleName = response.getClass().getSimpleName();
        CacheToken<R> newToken = CacheToken.newStatus(response.getCacheToken(), status);
        response.setCacheToken(newToken);
        
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
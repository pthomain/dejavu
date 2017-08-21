package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;

@AutoValue
public abstract class CacheToken<R> {
    
    public enum Status {
        DO_NOT_CACHE(true, true),
        //use on requests that should not be cached
        CACHE(true, true),
        //use on requests that should be cached
        NOT_CACHED(true, true),
        //use on requests that were not cached
        FRESH(true, true),
        //returned with responses coming straight from the network
        CACHED(true, true),
        //returned with responses coming straight from the cache within their expiry date
        STALE(false, false),
        //returned with responses coming straight from the cache after their expiry date
        REFRESHED(true, false),
        //returned after a STALE response with FRESH data from a successful network call
        COULD_NOT_REFRESH(true,
                          false
        );//returned after a STALE response with STALE data from an unsuccessful network call
        
        //Final responses will not be succeeded by any other response as part of the same call,
        //while non-final responses will be followed by at least another response.
        public final boolean isFinal;
        
        //Single responses are final and are not preceded or succeeded by another response.
        public final boolean isSingle;
        
        Status(boolean isFinal,
               boolean isSingle) {
            this.isSingle = isSingle;
            this.isFinal = isSingle || isFinal;
        }
    }
    
    public interface Holder<R> {
        void setCacheToken(@NonNull CacheToken<R> cacheToken);
        
        @NonNull
        CacheToken<? extends R> getCacheToken();
    }
    
    public static <R> CacheToken<R> newRequest(@NonNull Class<R> responseClass,
                                               @NonNull String apiUrl,
                                               @NonNull String... uniqueFields) {
        return new AutoValue_CacheToken<>(
                apiUrl,
                Status.CACHE,
                responseClass,
                Arrays.asList(uniqueFields),
                null,
                null,
                null,
                null
        );
    }
    
    public static <R> CacheToken<R> doNotCache(Class<R> responseClass) {
        return new AutoValue_CacheToken<>(
                "",
                Status.DO_NOT_CACHE,
                responseClass,
                new ArrayList<>(),
                null,
                null,
                null,
                null
        );
    }
    
    static <R> CacheToken<R> notCached(CacheToken<R> cacheToken,
                                       @NonNull Observable<R> refreshObservable,
                                       @NonNull Date fetchDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                Status.NOT_CACHED,
                cacheToken.getResponseClass(),
                cacheToken.getUniqueFields(),
                fetchDate,
                null,
                null,
                refreshObservable
        );
    }
    
    static <R> CacheToken<R> caching(CacheToken<R> cacheToken,
                                     @NonNull Observable<R> refreshObservable,
                                     @NonNull Date fetchDate,
                                     @NonNull Date cacheDate,
                                     @NonNull Date expiryDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                Status.FRESH,
                cacheToken.getResponseClass(),
                cacheToken.getUniqueFields(),
                fetchDate,
                cacheDate,
                expiryDate,
                refreshObservable
        );
    }
    
    static <R> CacheToken<R> cached(CacheToken<R> cacheToken,
                                    @NonNull Observable<R> refreshObservable,
                                    @NonNull Date cacheDate,
                                    @NonNull Date expiryDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                Status.CACHED,
                cacheToken.getResponseClass(),
                cacheToken.getUniqueFields(),
                cacheDate,
                cacheDate,
                expiryDate,
                refreshObservable
        );
    }
    
    static <R> CacheToken<R> newStatus(CacheToken<R> cacheToken,
                                       Status status) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                status,
                cacheToken.getResponseClass(),
                cacheToken.getUniqueFields(),
                cacheToken.getFetchDate(),
                cacheToken.getCacheDate(),
                cacheToken.getExpiryDate(),
                cacheToken.getRefreshObservable()
        );
    }
    
     String getKey() {
        StringBuilder builder = new StringBuilder();
        builder.append(getApiUrl());
        for (String field : getUniqueFields()) {
            builder.append("$");
            builder.append(String.valueOf(field));
        }
        return builder.toString();
    }
    
    public abstract String getApiUrl();
    
    public abstract Status getStatus();
    
    public abstract Class<R> getResponseClass();
    
    public abstract List<String> getUniqueFields();
    
    @Nullable
    public abstract Date getFetchDate();
    
    @Nullable
    public abstract Date getCacheDate();
    
    @Nullable
    public abstract Date getExpiryDate();
    
    @Nullable
    public abstract Observable<R> getRefreshObservable();
    
    @Override
    public abstract String toString();
}
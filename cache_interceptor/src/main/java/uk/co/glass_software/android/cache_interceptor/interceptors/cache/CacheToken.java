package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.Date;

import io.reactivex.Observable;

@AutoValue
public abstract class CacheToken<R> {
    
    public enum Status {
        //use on requests that should not be cached
        DO_NOT_CACHE(true, true),
        //use on requests that should be cached
        CACHE(true, true),
        //use on requests that should be refreshed regardless of whether they've expired
        REFRESH(true, true),
        //use on requests that were not cached
        NOT_CACHED(true, true),
        //returned with responses coming straight from the network
        FRESH(true, true),
        //returned with responses coming straight from the cache within their expiry date
        CACHED(true, true),
        //returned with responses coming straight from the cache after their expiry date
        STALE(false, false),
        //returned after a STALE response with FRESH data from a successful network call
        REFRESHED(true, false),
        //returned after a STALE response with STALE data from an unsuccessful network call
        COULD_NOT_REFRESH(true, false);
        
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
                                               @Nullable String body,
                                               float ttlInMinutes) {
        return new AutoValue_CacheToken<>(
                apiUrl,
                body,
                ttlInMinutes,
                Status.CACHE,
                responseClass,
                null,
                null,
                null,
                null
        );
    }
    
    public static <R> CacheToken<R> refresh(@NonNull Class<R> responseClass,
                                            @NonNull String apiUrl,
                                            @Nullable String body,
                                            float ttlInMinutes) {
        return new AutoValue_CacheToken<>(
                apiUrl,
                body,
                ttlInMinutes,
                Status.REFRESH,
                responseClass,
                null,
                null,
                null,
                null
        );
    }
    
    public static <R> CacheToken<R> doNotCache(@NonNull Class<R> responseClass) {
        return new AutoValue_CacheToken<>(
                "",
                null,
                0,
                Status.DO_NOT_CACHE,
                responseClass,
                null,
                null,
                null,
                null
        );
    }
    
    static <R> CacheToken<R> notCached(@NonNull CacheToken<R> cacheToken,
                                       @NonNull Observable<R> refreshObservable,
                                       @NonNull Date fetchDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                cacheToken.getBody(),
                cacheToken.getTtlInMinutes(),
                Status.NOT_CACHED,
                cacheToken.getResponseClass(),
                fetchDate,
                null,
                null,
                refreshObservable
        );
    }
    
    
    static <R> CacheToken<R> caching(@NonNull CacheToken<R> cacheToken,
                                     @NonNull Observable<R> refreshObservable,
                                     @NonNull Date fetchDate,
                                     @NonNull Date cacheDate,
                                     @NonNull Date expiryDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                cacheToken.getBody(),
                cacheToken.getTtlInMinutes(),
                Status.FRESH,
                cacheToken.getResponseClass(),
                fetchDate,
                cacheDate,
                expiryDate,
                refreshObservable
        );
    }
    
    static <R> CacheToken<R> cached(@NonNull CacheToken<R> cacheToken,
                                    @NonNull Observable<R> refreshObservable,
                                    @NonNull Date cacheDate,
                                    @NonNull Date expiryDate) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                cacheToken.getBody(),
                cacheToken.getTtlInMinutes(),
                Status.CACHED,
                cacheToken.getResponseClass(),
                cacheDate,
                cacheDate,
                expiryDate,
                refreshObservable
        );
    }
    
    static <R> CacheToken<R> newStatus(@NonNull CacheToken<R> cacheToken,
                                       @NonNull Status status) {
        return new AutoValue_CacheToken<>(
                cacheToken.getApiUrl(),
                cacheToken.getBody(),
                cacheToken.getTtlInMinutes(),
                status,
                cacheToken.getResponseClass(),
                cacheToken.getFetchDate(),
                cacheToken.getCacheDate(),
                cacheToken.getExpiryDate(),
                cacheToken.getRefreshObservable()
        );
    }
    
    String getKey(Hasher hasher) {
        String apiUrl = getApiUrl();
        String body = getBody();
        
        int urlHash = apiUrl.hashCode();
        try {
            if (body == null) {
                return hasher.hash(apiUrl);
            }
            else {
                return hasher.hash(apiUrl + "$" + body);
            }
        }
        catch (Exception e) {
            if (body == null) {
                return urlHash + "";
            }
            else {
                return String.valueOf(urlHash * 7 + body.hashCode());
            }
        }
    }
    
    protected abstract String getApiUrl();
    
    @Nullable
    protected abstract String getBody();
    
    public abstract float getTtlInMinutes();
    
    public abstract Status getStatus();
    
    public abstract Class<R> getResponseClass();
    
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
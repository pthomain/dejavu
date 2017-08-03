package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import java.io.IOException;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class ApiError extends Exception implements Function<ApiError, Boolean> {
    
    private final Throwable throwable;
    
    public ApiError(Throwable throwable) {
        this.throwable = throwable;
    }
    
    @Override
    public Boolean get(ApiError apiError) {
        return apiError.isNetworkError();
    }
    
    private Boolean isNetworkError() {
        return throwable instanceof IOException;
    }
}

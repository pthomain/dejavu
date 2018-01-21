package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.NonNull;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;

import retrofit2.HttpException;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError.NON_HTTP_STATUS;
import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.NETWORK;
import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.NOT_FOUND;
import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.UNAUTHORISED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.UNEXPECTED_RESPONSE;
import static uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.UNKNOWN;

public class ApiErrorFactory implements Function<Throwable, ApiError> {
    
    @Override
    public ApiError get(Throwable throwable) {
        if (throwable instanceof IOException || throwable instanceof JsonParseException) {
            return getIoError(throwable);
        }
        else if (throwable instanceof HttpException) {
            return getHttpError((HttpException) throwable);
        }
        else {
            ApiError apiError = ApiError.from(throwable);
            
            if (apiError == null) {
                return getError(
                        NON_HTTP_STATUS,
                        UNKNOWN,
                        throwable.getClass().getName()
                        + ": "
                        + throwable.getMessage(),
                        throwable
                );
            }
            
            return apiError;
        }
    }
    
    @NonNull
    private ApiError getHttpError(HttpException throwable) {
        ErrorCode errorCode = parseErrorCode(throwable);
        
        return getError(throwable.code(),
                        errorCode,
                        throwable.message(),
                        throwable
        );
    }
    
    @NonNull
    private ApiError getIoError(Throwable throwable) {
        if (throwable instanceof MalformedJsonException
            || throwable instanceof JsonParseException) {
            return getError(NON_HTTP_STATUS,
                            UNEXPECTED_RESPONSE,
                            throwable.getMessage(),
                            throwable
            );
        }
        else {
            return getError(NON_HTTP_STATUS,
                            NETWORK,
                            throwable.getMessage(),
                            throwable
            );
        }
    }
    
    @NonNull
    private ApiError getError(int httpStatus,
                              ErrorCode errorCode,
                              String rawDescription,
                              Throwable cause) {
        return new ApiError(
                cause,
                httpStatus,
                errorCode,
                rawDescription
        );
    }
    
    private ErrorCode parseErrorCode(HttpException httpException) {
        switch (httpException.code()) {
            case 401:
                return UNAUTHORISED;
            
            case 404:
                return NOT_FOUND;
            
            default:
                return UNKNOWN;
        }
    }
}

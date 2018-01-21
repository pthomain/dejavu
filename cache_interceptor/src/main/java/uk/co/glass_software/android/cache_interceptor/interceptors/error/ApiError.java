package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class ApiError extends Exception implements Function<ApiError, Boolean> {
    
    public final static int NON_HTTP_STATUS = -1;
    
    private final Throwable throwable;
    private final int httpStatus;
    private final ErrorCode errorCode;
    @Nullable private final String description;
    
    public ApiError(Throwable throwable) {
        this(throwable,
             NON_HTTP_STATUS,
             ErrorCode.UNKNOWN,
             null
        );
    }
    
    public ApiError(Throwable throwable,
                    int httpStatus,
                    ErrorCode errorCode,
                    @Nullable String description) {
        this.throwable = throwable;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.description = description;
    }
    
    @Override //isNetworkError
    public Boolean get(ApiError apiError) {
        return apiError.isNetworkError();
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    @Nullable
    public String getDescription() {
        return description;
    }
    
    public Boolean isNetworkError() {
        return errorCode == ErrorCode.NETWORK;
    }
    
    @Nullable
    public static ApiError from(Throwable throwable) {
        if (throwable instanceof ApiError) {
            return (ApiError) throwable;
        }
        return null;
    }
    
    public static boolean isNetworkError(Throwable throwable) {
        ApiError apiError = from(throwable);
        return apiError != null && apiError.isNetworkError();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        ApiError apiError = (ApiError) o;
        
        return new EqualsBuilder()
                .append(httpStatus, apiError.httpStatus)
                .append(throwable, apiError.throwable)
                .append(errorCode, apiError.errorCode)
                .append(description, apiError.description)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(throwable)
                .append(httpStatus)
                .append(errorCode)
                .append(description)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("throwable", throwable)
                .append("httpStatus", httpStatus)
                .append("errorCode", errorCode)
                .append("description", description)
                .toString();
    }
}
package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class ApiError extends Exception implements Function<ApiError, Boolean> {
    
    private final Throwable throwable;
    @Nullable private final String description;
    
    public ApiError(Throwable throwable) {
        this(throwable, null);
    }
    
    public ApiError(Throwable throwable,
                    @Nullable String description) {
        this.throwable = throwable;
        this.description = description;
    }
    
    @Override //isNetworkError
    public Boolean get(ApiError apiError) {
        return apiError.isNetworkError();
    }
    
    private Boolean isNetworkError() {
        return throwable instanceof IOException;
    }
    
    @Nullable
    public String getDescription() {
        return description;
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
                .append(throwable, apiError.throwable)
                .append(description, apiError.description)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(throwable)
                .append(description)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("throwable", throwable)
                .append("description", description)
                .toString();
    }
}
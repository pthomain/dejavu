package uk.co.glass_software.android.cache_interceptor.interceptors.error;

public enum ErrorCode {
    
    NETWORK(true),
    UNAUTHORISED(false),
    NOT_FOUND(false),
    UNEXPECTED_RESPONSE(true),
    UNKNOWN(true);
    
    public final boolean isRecoverable;
    
    ErrorCode(boolean isRecoverable) {
        this.isRecoverable = isRecoverable;
    }
}

package uk.co.glass_software.android.utils;

public interface Logger {
    void e(Object caller,
           Throwable t,
           String message);
    
    void e(Object caller,
           String message);
    
    void d(Object caller,
           String message);
    
    final class LogException extends Exception {
        public LogException(String detailMessage) {
            super(detailMessage);
        }
    }
}

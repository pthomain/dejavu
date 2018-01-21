package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import retrofit2.HttpException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiErrorFactoryUnitTest {
    
    private ApiErrorFactory target;
    
    @Before
    public void setUp() throws Exception {
        target = new ApiErrorFactory();
    }
    
    @Test
    public void testParseMalformedJsonException() {
        MalformedJsonException exception = new MalformedJsonException("malformed");
     
        ApiError apiError = target.get(exception);
        
        assertApiError(
                apiError,
                "malformed",
                ErrorCode.UNEXPECTED_RESPONSE,
                -1,
                false
        );
    }
    
    @Test
    public void testParseIOException() {
        IOException exception = new IOException("time out");
  
        ApiError apiError = target.get(exception);
        
        assertApiError(
                apiError,
                "time out",
                ErrorCode.NETWORK,
                -1,
                true
        );
    }
    
    @Test
    public void testParseHttpExceptionUnauthorised() {
        HttpException exception = mock(HttpException.class);
        when(exception.code()).thenReturn(401);
        when(exception.message()).thenReturn("not authorised");
        
        ApiError apiError = target.get(exception);
     
        assertApiError(
                apiError,
                "not authorised",
                ErrorCode.UNAUTHORISED,
                401,
                false
        );
    }
    
    @Test
    public void testParseHttpExceptionUnknown() {
        HttpException exception = mock(HttpException.class);
        when(exception.code()).thenReturn(500);
        when(exception.message()).thenReturn("unknown");
        
        ApiError apiError = target.get(exception);
        
        assertApiError(
                apiError,
                "unknown",
                ErrorCode.UNKNOWN,
                500,
                false
        );
    }
    
    public static void assertApiError(ApiError error,
                                      String expectedRawDescription,
                                      ErrorCode expectedErrorCode,
                                      int expectedHttpCode,
                                      boolean isNetworkError) {
        assertNotNull("error should not be null", error);
        
        String rawDescription = error.getDescription();
        
        int httpStatus = error.getHttpStatus();
        assertEquals("httpStatus should be " + expectedHttpCode, expectedHttpCode, httpStatus);
        
        if (httpStatus == 200) {
            if (expectedRawDescription == null) {
                assertNull("rawDescription should be null, was: " + rawDescription, rawDescription);
            }
            else {
                assertNotNull("rawDescription should not be null", rawDescription);
                assertTrue("rawDescription should contain message: " + expectedRawDescription,
                           rawDescription.contains(expectedRawDescription)
                );
            }
        }
        
        assertEquals("Expected network error == " + isNetworkError,
                     isNetworkError,
                     (boolean) error.isNetworkError()
        );
        
        ErrorCode errorCode = error.getErrorCode();
        assertNotNull("errorCode should not be null", errorCode);
        assertEquals("errorCode should be " + expectedErrorCode, expectedErrorCode, errorCode);
    }
}
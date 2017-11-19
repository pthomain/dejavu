package uk.co.glass_software.android.cache_interceptor.base.network;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;

import static junit.framework.Assert.fail;
import static okhttp3.Protocol.HTTP_1_1;

/**
 * This class provides a way to enqueue HTTP responses for mocking purposes in integration tests.
 * It works in conjunction with the addition of an interceptor to the OkHttpClient.
 * <p>
 * See https://github.com/square/okhttp/issues/1096
 */
public final class MockClient implements Interceptor {
    
    private Deque<Object> events = new ArrayDeque<>();
    private Deque<Request> requests = new ArrayDeque<>();
    
    /**
     * Does the interception of the request and bypasses the network call by returning the mocked
     * responses or exception enqueued prior to the network call being made.
     * Refer to OkHttp documentation for more information.
     */
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        requests.addLast(request);
        
        Object event;
        
        try {
            event = events.removeFirst();
        }
        catch (NoSuchElementException nse) {
            fail("No request in queue");
            return null;
        }
        
        if (event instanceof IOException) {
            throw (IOException) event;
        }
        if (event instanceof RuntimeException) {
            throw (RuntimeException) event;
        }
        if (event instanceof ResponseWrapper) {
            ResponseWrapper responseWrapper = (ResponseWrapper) event;
            return responseWrapper.responseBuilder
                    .request(request)
                    .protocol(HTTP_1_1)
                    .code(responseWrapper.httpCode)
                    .build();
        }
        throw new IllegalStateException("Unknown event " + event.getClass());
    }
    
    /**
     * Enqueues a new HTTP response to be delivered in order during the test.
     *
     * @param responseWrapper the response wrapper to be enqueued
     */
    public void enqueueResponse(ResponseWrapper responseWrapper) {
        events.addLast(responseWrapper);
    }
    
    /**
     * Delegates call to MockClient for convenience
     *
     * @param response the mocked HTTP response body
     * @param httpCode the mocked HTTP response status code
     * @see MockClient#enqueueResponse(MockClient.ResponseWrapper)
     */
    public void enqueueResponse(String response,
                                int httpCode) {
        Response.Builder responseBuilder = new Response.Builder();
        Buffer source = new Buffer();
        source.write(response.getBytes());
        Headers.Builder headerBuilder = new Headers.Builder();
        RealResponseBody body = new RealResponseBody(headerBuilder.build(), source);
        responseBuilder.body(body);
        enqueueResponse(new MockClient.ResponseWrapper(responseBuilder, httpCode));
    }
    
    /**
     * Enqueues a RuntimeException to be thrown in order during the test.
     *
     * @param exception the exception to be thrown
     */
    public void enqueueRuntimeException(RuntimeException exception) {
        events.addLast(exception);
    }
    
    /**
     * Enqueues a IOException to be thrown in order during the test.
     *
     * @param exception the exception to be thrown
     */
    public void enqueueIOException(IOException exception) {
        events.addLast(exception);
    }
    
    public List<Request> getRequestHistory() {
        return new LinkedList<>(requests);
    }
    
    public void clearRequestHistory() {
        requests.clear();
    }
    
    /**
     * Class used to wrap the Response.Builder along with the mocked HTTP status code, since this
     * is not available on the builder itself.
     */
    public static class ResponseWrapper {
        private final Response.Builder responseBuilder;
        private final int httpCode;
        
        public ResponseWrapper(Response.Builder responseBuilder,
                               int httpCode) {
            this.responseBuilder = responseBuilder;
            this.httpCode = httpCode;
        }
        
        public Response.Builder getResponseBuilder() {
            return responseBuilder;
        }
        
        public int getHttpCode() {
            return httpCode;
        }
    }
}
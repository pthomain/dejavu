package uk.co.glass_software.android.cache_interceptor.retrofit;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class RetrofitCacheAdapterUnitTest {
    
    private RxCacheInterceptor.Factory mockInterceptorFactory;
    private CallAdapter mockCallAdapter;
    
    private RetrofitCacheAdapter target;
    
    @Before
    public void setUp() throws Exception {
        mockInterceptorFactory = mock(RxCacheInterceptor.Factory.class);
        mockCallAdapter = mock(CallAdapter.class);
        
        target = new RetrofitCacheAdapter(
                mockInterceptorFactory,
                TestResponse.class,
                mockCallAdapter
        );
    }
    
    @Test
    public void testAdapt() throws Exception {
        Call mockCall = mock(Call.class);
        HttpUrl url = HttpUrl.parse("http://test.com");
        RequestBody mockBody = RequestBody.create(MediaType.parse("text/plain"), "body");
        Request mockRequest = new Request.Builder().url(url).put(mockBody).build();
        RxCacheInterceptor mockCacheInterceptor = mock(RxCacheInterceptor.class);
        TestResponse mockUpstreamResponse = mock(TestResponse.class);
        TestResponse mockDownstreamResponse = mock(TestResponse.class);
        Observable<TestResponse> mockUpstreamObservable = Observable.just(mockUpstreamResponse);
        Observable<TestResponse> mockDownstreamObservable = Observable.just(mockDownstreamResponse);
        
        when(mockCallAdapter.adapt(eq(mockCall))).thenReturn(mockUpstreamObservable);
        when(mockCall.request()).thenReturn(mockRequest);
        when(mockCacheInterceptor.apply(eq(mockUpstreamObservable))).thenReturn(mockDownstreamObservable);
        
        doReturn(mockCacheInterceptor).when(mockInterceptorFactory).create(
                eq(TestResponse.class),
                eq(url.toString()),
                eq(mockBody.toString())
        );
        
        assertEquals(mockDownstreamResponse, ((Observable<TestResponse>) target.adapt(mockCall)).blockingFirst());
    }
}
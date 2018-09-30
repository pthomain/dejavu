
package uk.co.glass_software.android.cache_interceptor.base.network.model;

import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.base.BaseCachedList;

public class TestResponse extends BaseCachedList<ApiError, TestResponse, User> {
    
    public static String STUB_FILE = "api_stub_test.json";
    public static String URL = "http://test.com";
    
}


package uk.co.glass_software.android.cache_interceptor.base.network.model;

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.CachedList;

public class TestResponse extends CachedList<ApiError, TestResponse, User> {
    
    public static String STUB_FILE = "api_stub_test.json";
    
}

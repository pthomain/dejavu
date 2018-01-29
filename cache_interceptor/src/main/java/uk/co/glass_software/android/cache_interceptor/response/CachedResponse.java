package uk.co.glass_software.android.cache_interceptor.response;

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.base.BaseCachedResponse;

public abstract class CachedResponse<R> extends BaseCachedResponse<ApiError, R> {
}

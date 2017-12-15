# RxCacheInterceptor (beta)
**A versatile RxJava/Retrofit HTTP cache**

TL;DR
=====

Enable offline access to your app and speed up screen loading by caching each of your API responses for a specific time.
All responses are automatically cached to disk and refreshed once they expire. 
Snappy compression and AES encryption are supported and optional.

Retrofit support
================

This library provides an adapter to be used during the setup of Retrofit which handles the cache transparently.
This means caching can be added to existing codebases using Retrofit/RxJava with minimal effort and very little refactoring.

Volley / other networking lib support
=====================================

It is possible to use the cache interceptor with other networking libs, take a look at the demo app for an example implementation for Volley.
The documentation below will refer exclusively to the Retrofit implementation.

*Note: This library needs to be used in conjunction with RxJava and Gson.*

Overview
========

Replace ```RxJava2CallAdapterFactory``` with ```RetrofitCacheAdapterFactory``` during your Retrofit setup:

```java
Retrofit retrofit = new Retrofit.Builder()
                                /* ... usual setup */ 
                                .addCallAdapterFactory(RetrofitCacheAdapterFactory.build(context))
                                .build();
```

Assuming that your Retrofit client looks like:

```java
interface UserClient {  

  @GET("/user/{userId}")
  Observable<UserResponse> get(@Path("userId") String userId);
  
}
```

Then you need to define you ```UserResponse``` class as such:

```java
  public class UserResponse extends CachedResponse<ApiError, UserResponse> {
    
    @Override
    public float getTtlInMinutes() {
      return 5f; //Determines how long to cache this request for
    }
    
    /* Add your response fields below */
}
```

Advanced configuration
======================

If for your response classes are already extending from a parent class (say ```BaseResponse```), then implement the ```ResponseMetadata.Holder``` interface instead:

```java
  public class UserResponse extends BaseResponse implements ResponseMetadata.Holder<ApiError, UserResponse> {
  
    private transient ResponseMetadata<UserResponse, ApiError> metadata; //transient is important here, ResponseMetadata is not serializable
    
    @Override
    public float getTtlInMinutes() {
      return 5f;
    }
    
    @Override
    public boolean isRefresh() {
      return false; //or true if you want this response to always force a refresh
    }
    
    @NonNull
    @Override
    public ResponseMetadata<UserResponse, ApiError> getMetadata() {
      return metadata;
    }
    
    @Override
    public void setMetadata(ResponseMetadata<UserResponse, ApiError> metadata) {
      this.metadata = metadata;
    }
    
    /* Add your response fields below */
	}
```

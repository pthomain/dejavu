# RxCacheInterceptor
**A simple RxJava/Retrofit HTTP cache**

TL;DR
-----

Enable offline access and speed up screen loading by caching your API responses for a specific amount of time.
All responses are automatically cached to disk and refreshed once they expire. 
Snappy compression (https://github.com/google/snappy) and AES encryption are supported and optional.

<a href="https://play.google.com/store/apps/details?id=uk.co.glass_software.android.cache_interceptor.demo"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="250"/></a>

Retrofit support
----------------

This library provides an adapter to be used during the setup of Retrofit which handles the cache transparently.
This means caching can be added to existing codebases using Retrofit/RxJava with minimal effort and almost no refactoring.

Volley / other networking lib support
-------------------------------------

It is possible to use the cache interceptor with other networking libs, take a look at the demo app for an example implementation for Volley.

The documentation below will refer exclusively to the Retrofit implementation.

*Note: This library needs to be used in conjunction with RxJava and Gson.*

Adding the dependency
---------------------

To add the library to your project, add the following block to your root gradle file:

```
allprojects {
 repositories {
    jcenter()
    maven { url "https://jitpack.io" }
 }
}
 ```
 
 Then add the following dependency to your module:
 
 ```
 dependencies {
    compile 'com.github.pthomain:RxCacheInterceptor:1.0.0'
}
```

Overview
--------

Just replace ```RxJava2CallAdapterFactory``` with ```RetrofitCacheAdapterFactory``` during your Retrofit setup:

```java
Retrofit retrofit = new Retrofit.Builder()
                                /* ... usual setup */ 
                                .addCallAdapterFactory(RetrofitCacheAdapterFactory.build(context))
                                .build();
```

Assuming that your Retrofit client looks like this:

```java
interface UserClient {  

  @GET("/user/{userId}")
  Observable<UserResponse> get(@Path("userId") String userId);
  
}
```

Then you need to define your ```UserResponse``` class as such:

```java
  public class UserResponse extends CachedResponse<ApiError, UserResponse> {
    
    //This method only needs to be overriden if you want to implement a different
    //TTL than the default one, which is set to 5 min.
    @Override	
    public float getTtlInMinutes() {
      return 10f; //Determines how long to cache this request for
    }
    
    /* Add your response fields below */
}
```

And that's it. Any call to ```UserClient.get()``` will return a ```FRESH``` response for the first call followed by a ```CACHED``` response (straight from the disk) for any subsquent called made during the TTL interval from the point the request was last refreshed.

CachedResponse
--------------

The simplest way to enable caching on your responses is to have them extend from the ```CachedResponse``` object.
This class contains all the needed metadata, including the duration for which to cache the response.

See the [Advanced configuration](#advanced-configuration) section if your responses are already extending from a base class or for special cases.

Request metadata
----------------

```CachedResponse``` contains the following methods to be overridden if needed:

- ```getTtlInMinutes()```: returns how long to cache responses for (5 minutes by default).
- ```isRefresh()```: set to true to force a refresh of the response regardless of how old it is (false by default).
- ```getMetadata()```: contains metadata related to errors and cache status. 
- ```splitOnNextOnError()```: see the [Error handling](#error-handling) section.

CacheToken
----------

```CacheToken``` is available on the ```ResponseMetadata``` object for both requests and responses.
It provides information about the cache status of the response or act as an instruction to the ```CacheManager```.

A request ```CacheToken``` can be:
- ```CACHE```: default value, instructs the ```CacheManager``` to cache the response. 
- ```DO_NOT_CACHE```: instructs the ```CacheManager``` not to cache the response.
- ```REFRESH```: instructs the ```CacheManager``` to invalidate the cached response and request a new response from the API.

A response ```CacheToken``` can be:
- ```NOT_CACHED```(final): indicates that the response was not cached (as a result of a ```DO_NOT_CACHE``` request token)
- ```FRESH```(final): indicates that the response is coming straight from the network
- ```CACHED```(final): indicates that the response is coming straight from the cache (within their expiry window)
- ```STALE```(*non-final*): indicates a response coming straight from the cache (after their expiry date)
- ```REFRESHED```(final): returned after a STALE response with FRESH data from a successful network call
- ```COULD_NOT_REFRESH```(final): returned after a STALE response with STALE data from an unsuccessful network call

*IMPORTANT: All response ```CacheToken``` are final except for ```STALE```*

This means that ```STALE``` is the only "temporary response" and is emitted to onNext() while another API call is attempted.
This allows the possibility to show expired data in the UI while it is being refreshed (if applicable).

What it also means is that another response will follow: either ```REFRESHED``` or ```COULD_NOT_REFRESH``` depending on the success of the subsequent API call.

If the ```STALE``` data is irrelevant, then it should be filtered like so: 
```java
cachedResponseObservable.filter(response -> response.getMetadata().getCacheToken().getStatus().isFinal)
```
Responses containing errors are never cached.

For more explanation about the ```CacheToken``` mechanism, see the [Sequence diagrams](#sequence-diagrams) section.

Error handling
--------------

By default, this interceptor overrides the default RxJava error handling mechanism and does not emit errors.
Instead any error emitted upstream is intercepted and re-delivered as an ApiError object in the response metadata. 

Because of this, developers would always expect a valid response through onNext() for any call made even if it resulted in an error and as such should always check that the response metadata has no error (hasError() == false) before trying to read the response's fields.

This simplifies error handling in most cases by allowing the observable to only be subscribed with a single callback for onNext().

If you would rather use both the onNext() and onError() callbacks, override the ```splitOnNextOnError()``` method on the CachedResponse object to return true.

Advanced configuration
----------------------

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

Custom JSON response deserialisation
------------------------------------


Sequence diagrams
-----------------

### CACHE → FRESH

![CACHE → FRESH](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/cache_fresh.png)

### CACHE → CACHED

![CACHE → CACHED](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/cache_cached.png)

### CACHE → STALE → REFRESHED

![CACHE → STALE → REFRESHED](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/cache_refreshed.png)

### CACHE → STALE → COULD_NOT_REFRESH

![CACHE → STALE → COULD_NOT_REFRESH](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/cache_could_not_refresh.png)

### REFRESH → REFRESHED

![REFRESH → REFRESHED](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/refresh_refreshed.png)

### REFRESH → COULD_NOT_REFRESH

![REFRESH → COULD_NOT_REFRESH](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/refresh_could_not_refresh.png)

### DO_NOT_CACHE → NOT_CACHED

![DO_NOT_CACHE → NOT_CACHED](https://github.com/pthomain/RxCacheInterceptor/blob/master/github/diagrams/do_not_cache.png)




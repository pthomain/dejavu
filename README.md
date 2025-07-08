[![Codacy Badge](https://api.codacy.com/project/badge/Grade/21c1e62561044bf49195b21e8ce3aa02)](https://app.codacy.com/manual/pthomain/dejavu?utm_source=github.com&utm_medium=referral&utm_content=pthomain/dejavu&utm_campaign=Badge_Grade_Dashboard)[![](https://jitpack.io/v/pthomain/dejavu.svg)](https://jitpack.io/#pthomain/dejavu) [![Known Vulnerabilities](https://snyk.io/test/github/pthomain/dejavu/badge.svg)](https://snyk.io/test/github/pthomain/dejavu)

DejaVu provides a locally controlled cache for API responses. It is used to:

- reduce the need for unnecessary network calls
- speed up UI loading by displaying previous data while new data is being fetched
- provide offline data when the network is unavailable

It is fully customisable and supports compression / encryption. 

What sets it apart is that it is designed to work with as little setup as possible and does not require any refactoring of your existing code.
You can start caching your Retrofit calls simply by adding an annotation to the existing client's methods without needing to change their signature or any call handling code. 

Alternatively, you can leave your clients' code entirely untouched and decide which call to cache be implementing a cache predicate which will intercept any request and let you decide ad hoc caching rules before the network call is made.
All requests are cached uniquely based on the query parameters and response model class. The cache can also be invalidated or cleared on a per request basis (taking the original parameters into account for request uniqueness).

This library's goal is to introduce no side effect to the existing code and it was designed to be added or removed completely transparently.
This is achieved by swapping the default RxJava call adapter factory on Retrofit with the one the library provides, which constrains all the needed changes to the Retrofit setup.

There is support for customisable encryption (choice of JetPack Security on 23+, Facebook Conceal on 16+ or any preferred custom implementation) and Snappy compression (https://github.com/google/snappy).

Code example
-----------------

Set the library up, here with encryption:

```kotlin
val dejaVu = DejaVu.builder()
                   .encryption(if (SDK_INT >= 23) Mumbo::tink else Mumbo::conceal)
                   .build(context, GsonSerialiser(gson))
```

Update your Retrofit setup:

```kotlin
val retrofit = Retrofit.Builder()
                     /** Usual setup goes here **/
                    // Swap your default RxJava call adapter factory
                    .addCallAdapterFactory(dejaVu.retrofitCallAdapterFactory) 
                    .build()
```

Update your existing Retrofit client by adding an annotation to the call you want to cache:

```kotlin
interface UserClient {

    @GET("users")
    @Cache(durationInSeconds = 300) // DejaVu cache annotation 
    fun getUsers(
        status: UserStatus = ACTIVE,
        limit : Int = 20    
    ): Single<UserResponse>

}
```

Retrofit support
----------------

This library provides an adapter to be used during the setup of Retrofit which handles the cache transparently.
This means caching can be added to existing codebases using Retrofit/RxJava with minimal effort and almost no refactoring.

Volley / other networking lib support
-------------------------------------

It is possible to use the cache interceptor with other networking libs, take a look at the demo app for an example implementation for Volley.

The documentation below will refer exclusively to the Retrofit implementation.

Serialisation support
---------------------

You can provide your own serialisation by implementing the `Serialiser` interface. This needs to be the same implementation that you use to handle your API models.

Coroutines support
------------------

Coroutines are not currently supported but are on the roadmap. However, this library is using RxJava and coroutine support will still require RxJava as a dependency.

Adding the dependency [![](https://jitpack.io/v/pthomain/dejavu.svg)](https://jitpack.io/#pthomain/dejavu)
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
    compile 'com.github.pthomain:dejavu:2.1.0-beta1'
}
```

Documentation
-------------

Coming soon...

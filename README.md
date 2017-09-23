# RxCacheInterceptor (alpha)
## An RxJava/JSON/Retrofit HTTP cache

Provides an HTTP cache with client-side cache control, overriding the default HTTP Cache-Control directives set on the server-side.
This is useful when trying to cache responses coming from an API on which the cache control is absent or badly implemented.
It also allows for a simple offline cache mechanism.

Currently only JSON responses are supported. Responses are serialised using Gson, compressed using Snappy and saved to SQLite. 
Custom deserialisers must be provided if needed.

This repo is under active developement and is **not meant for production** at this stage.

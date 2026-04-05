# DejaVu 3.0.0

Dead-simple, transparent API caching for Android with Kotlin Coroutines and Flow.

## Features

- **Transparent caching** -- Just swap the CallAdapter factory (Retrofit) or install the plugin (Ktor)
- **Flow-based** -- Emits stale-then-fresh data as a reactive Flow
- **Multiple persistence backends** -- Room (SQLite) for persistent cache, in-memory for session cache
- **Annotation-driven** -- `@Cache`, `@DoNotCache`, `@Invalidate`, `@Clear`
- **Encryption** -- Optional AES-256-GCM encryption via Google Tink
- **Custom error handling** -- Pluggable `ErrorFactory` with `Outcome` sealed class
- **Minimal setup** -- Builder pattern with sensible defaults

## Quick Start

### Retrofit

```kotlin
// 1. Build DejaVu
val dejaVu = DejaVu.defaultBuilder(context).build()

// 2. Build DejaVu Retrofit
val dejaVuRetrofit = DejaVu.defaultBuilder(context)
    .extend(DejaVuRetrofit.extension())
    .build()

// 3. Add to Retrofit
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addCallAdapterFactory(dejaVuRetrofit.callAdapterFactory)
    .build()

// 4. Annotate your API
interface MyApi {
    @Cache(durationInSeconds = 300)
    @GET("users/{id}")
    fun getUser(@Path("id") id: String): Flow<DejaVuResult<User>>
}
```

### Ktor

```kotlin
val dejaVu = DejaVu.defaultBuilder(context).build()

val client = HttpClient(OkHttp) {
    install(DejaVuPlugin) {
        interceptorFactory = dejaVu.interceptorFactory
    }
}

// Use cache DSL on requests
val user: DejaVuResult<User> = client.get("/users/1") {
    cache(duration = 5.minutes, priority = FRESH_PREFERRED)
}.body()
```

## Cache Operations

| Annotation | Description |
|---|---|
| `@Cache(durationInSeconds)` | Cache response for specified duration |
| `@DoNotCache` | Skip caching for this call |
| `@Invalidate` | Mark cached data as stale |
| `@Clear` | Remove cached entries |

## Cache Priority

- `FRESH_ONLY` -- Only return fresh data, wait for network
- `FRESH_PREFERRED` -- Return fresh if available, stale as fallback
- `STALE_ACCEPTED` -- Return stale immediately, then fresh from network
- `OFFLINE` -- Only return cached data, never hit network

## Persistence

```kotlin
// Room (default) -- persistent across app restarts
DejaVu.defaultBuilder(context)
    .withPersistence(SqlitePersistence(context))
    .build()

// Memory -- session-only, lost on app termination
DejaVu.defaultBuilder(context)
    .withPersistence(MemoryPersistence())
    .build()
```

## Encryption

```kotlin
DejaVu.defaultBuilder(context)
    .withEncryption(Encryption(context)) // Tink AES-256-GCM
    .build()
```

## Modules

| Module | Artifact | Description |
|---|---|---|
| `lib:core` | `dejavu-core` | Core caching logic |
| `lib:modules:http:retrofit` | `dejavu-retrofit` | Retrofit integration |
| `lib:modules:http:ktor` | `dejavu-ktor` | Ktor client plugin |
| `lib:modules:persistence:sqlite` | `dejavu-persistence-room` | Room persistence |
| `lib:modules:persistence:memory` | `dejavu-persistence-memory` | In-memory persistence |
| `lib:modules:serialisation:kotlinx` | `dejavu-serialisation-kotlinx` | kotlinx.serialization |
| `lib:modules:serialisation:decorators:encryption` | `dejavu-encryption` | Tink encryption |

## Migration from v2

- RxJava `Observable<T>` -> Kotlin `Flow<T>`
- `Single<T>` -> `Flow<T>` (single emission)
- Koin DI -> `DejaVu.defaultBuilder(context).build()`
- Gson/Moshi -> kotlinx.serialization (data classes need `@Serializable`)
- Volley -> Use Ktor module instead
- File persistence -> Use Room (SQLite) module
- Mumbo encryption -> Tink encryption (automatic migration)
- Compression decorator -> Removed

## Requirements

- Android API 24+ (Nougat)
- Kotlin 2.1+
- Coroutines 1.10+

## License

Apache License 2.0

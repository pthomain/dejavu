package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import retrofit2.http.DELETE
import retrofit2.http.GET
import uk.co.glass_software.android.cache_interceptor.annotations.*
import uk.co.glass_software.android.cache_interceptor.annotations.OptionalBoolean.TRUE
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter.Companion.ENDPOINT

internal interface CatFactClient {

    // GET

    @GET(ENDPOINT)
    @Cache
    fun get(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(compress = TRUE)
    fun compressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(encrypt = TRUE)
    fun encrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            compress = TRUE,
            encrypt = TRUE
    )
    fun compressedEncrypted(): Observable<CatFactResponse>

    // GET freshOnly

    @GET(ENDPOINT)
    @Cache(freshOnly = true)
    fun freshOnly(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE
    )
    fun freshOnlyCompressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            encrypt = TRUE
    )
    fun freshOnlyEncrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE,
            encrypt = TRUE
    )
    fun freshOnlyCompressedEncrypted(): Observable<CatFactResponse>

    // REFRESH

    @GET(ENDPOINT)
    @Refresh
    fun refresh(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Refresh(freshOnly = true)
    fun refreshFreshOnly(): Observable<CatFactResponse>

    // CLEAR

    @DELETE(ENDPOINT)
    @Clear(typeToClear = CatFactResponse::class)
    fun clearCache(): Completable

    // INVALIDATE

    @DELETE(ENDPOINT)
    @Invalidate(typeToInvalidate = CatFactResponse::class)
    fun invalidate(): Completable

    // OFFLINE

    @GET(ENDPOINT)
    @Offline
    fun offline(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Offline(freshOnly = true)
    fun offlineFreshOnly(): Observable<CatFactResponse>

}

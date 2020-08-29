package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiErrorFactory
import dev.pthomain.android.dejavu.serialisation.gson.GsonGlitchFactory
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.glitch.Glitch

sealed class ErrorFactoryType<E>(val errorFactory: ErrorFactory<E>)
        where E : Throwable,
              E : NetworkErrorPredicate {

    object Default : ErrorFactoryType<Glitch>(GsonGlitchFactory())
    object Custom : ErrorFactoryType<CustomApiError>(CustomApiErrorFactory())

}
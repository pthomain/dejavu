package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.GlitchFactory

sealed class ErrorFactoryType<E>(val errorFactory: ErrorFactory<E>)
        where E : Throwable,
              E : NetworkErrorPredicate {

    object Default : ErrorFactoryType<Glitch>(DejaVuGlitchFactory(GlitchFactory()))
    object Custom : ErrorFactoryType<CustomApiError>(CustomApiErrorFactory())

}

package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiErrorFactory
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.error.ErrorFactory
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.serialisation.gson.GsonErrorFactory

sealed class ErrorFactoryType<E>(val errorFactory: ErrorFactory<E>)
        where E : Throwable,
              E : NetworkErrorPredicate {

    object Default : ErrorFactoryType<DejaVuError>(GsonErrorFactory())
    object Custom : ErrorFactoryType<CustomApiError>(CustomApiErrorFactory())

}
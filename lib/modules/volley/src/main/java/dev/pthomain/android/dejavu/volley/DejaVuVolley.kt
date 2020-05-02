package dev.pthomain.android.dejavu.volley

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.configuration.DejaVuBuilder
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.utils.SilentLogger
import dev.pthomain.android.glitchy.core.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate

class DejaVuVolley<E> internal constructor(
        val interceptorFactory: DejaVuInterceptor.Factory<E>,
        val observableFactory : VolleyObservable.Factory<E>
) where E : Throwable,
        E : NetworkErrorPredicate {

    companion object {

        fun <E> extension()
                where E : Throwable,
                      E : NetworkErrorPredicate =
                DejaVuVolleyBuilder<E>()

        fun <E> builder(dejaVuBuilder: DejaVuBuilder<E>)
                where E : Throwable,
                      E : NetworkErrorPredicate =
                dejaVuBuilder.extend(extension<E>())

        fun <E> builder(
                context: Context,
                errorFactory: ErrorFactory<E>,
                persistenceManagerModule: PersistenceManager.ModuleProvider,
                logger: Logger = SilentLogger
        ) where E : Throwable,
                E : NetworkErrorPredicate =
                builder(
                        DejaVu.builder(
                                context,
                                errorFactory,
                                persistenceManagerModule,
                                logger
                        )
                )
    }

}
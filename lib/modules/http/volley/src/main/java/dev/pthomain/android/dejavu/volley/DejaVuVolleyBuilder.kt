package dev.pthomain.android.dejavu.volley

import dev.pthomain.android.dejavu.configuration.ExtensionBuilder
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.serialisation.Serialiser

class DejaVuVolleyBuilder<E> internal constructor(
        private val serialiser: Serialiser? = null
) : ExtensionBuilder<DejaVuVolleyBuilder<E>, DejaVuVolley<E>, E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    private var parentComponent: DejaVuComponent<E>? = null

    override fun accept(component: DejaVuComponent<E>) = apply {
        parentComponent = component
    }

    /**
     * Provides the serialiser needed for Volley response deserialisation.
     */
    fun withSerialiser(serialiser: Serialiser) = DejaVuVolleyBuilder<E>(serialiser).also {
        it.parentComponent = this.parentComponent
    }

    /**
     * Returns an instance of DejaVuVolley.
     */
    override fun build(): DejaVuVolley<E> {
        val component = this.parentComponent
                ?: throw IllegalStateException("This builder needs to call DejaVuBuilder::extend")

        val resolvedSerialiser = this.serialiser
                ?: throw IllegalStateException("A Serialiser must be provided via withSerialiser()")

        val volleyObservableFactory = VolleyObservable.Factory<E>(
                component.errorFactory,
                resolvedSerialiser,
                component.interceptorFactory
        )

        return DejaVuVolley(volleyObservableFactory)
    }
}

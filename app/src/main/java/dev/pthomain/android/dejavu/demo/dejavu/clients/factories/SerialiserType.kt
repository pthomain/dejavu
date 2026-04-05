package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.dejavu.serialisation.kotlinx.KotlinxSerialiser

sealed class SerialiserType(val serialiser: Serialiser) {

    object Kotlinx : SerialiserType(KotlinxSerialiser())
}

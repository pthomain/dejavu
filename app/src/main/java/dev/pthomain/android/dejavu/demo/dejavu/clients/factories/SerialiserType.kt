package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import com.google.gson.GsonBuilder
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.dejavu.serialisation.gson.GsonSerialiser
import dev.pthomain.android.dejavu.serialisation.moshi.MoshiSerialiser

sealed class SerialiserType(val serialiser: Serialiser) {

    object Gson : SerialiserType(GsonSerialiser(GsonBuilder().create()))

    object Moshi : SerialiserType(MoshiSerialiser(
            com.squareup.moshi.Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
    ))
}
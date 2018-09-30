package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation

import com.google.gson.Gson

import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser

internal class GsonSerialiser(private val gson: Gson) : Serialiser {

    override fun canHandleType(targetClass: Class<*>) = true
    override fun canHandleSerialisedFormat(serialised: String) = true

    @Throws(Serialiser.SerialisationException::class)
    override fun <O : Any> serialise(deserialised: O) = gson.toJson(deserialised)!!

    @Throws(Serialiser.SerialisationException::class)
    override fun <O> deserialise(serialised: String,
                                 targetClass: Class<O>) = gson.fromJson(serialised, targetClass)!!

}
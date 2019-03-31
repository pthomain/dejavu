package uk.co.glass_software.android.dejavu.demo.gson


import com.google.gson.JsonParseException
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorCode.UNEXPECTED_RESPONSE
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.dejavu.interceptors.internal.error.GlitchFactory

/**
 * Custom ErrorFactory implementation handling Gson specific errors
 */
class GsonGlitchFactory : GlitchFactory() {

    override fun getError(throwable: Throwable) =
            when (throwable) {
                is JsonParseException -> Glitch(
                        throwable,
                        NON_HTTP_STATUS,
                        UNEXPECTED_RESPONSE,
                        throwable.message
                )
                else -> super.getError(throwable)
            }

}

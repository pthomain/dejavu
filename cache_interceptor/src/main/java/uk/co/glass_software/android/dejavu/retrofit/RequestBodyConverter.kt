package uk.co.glass_software.android.dejavu.retrofit

import okhttp3.Request
import okio.Buffer
import java.io.IOException

/**
 * Converts a request's body to String
 *
 * @param request the OkHttp request
 * @return the request's body as a String
 */
class RequestBodyConverter : (Request) -> String? {
    override fun invoke(p1: Request) =
            try {
                Buffer().apply {
                    p1.newBuilder().build().body()?.writeTo(this)
                }.readUtf8()
            } catch (e: IOException) {
                null
            }
}
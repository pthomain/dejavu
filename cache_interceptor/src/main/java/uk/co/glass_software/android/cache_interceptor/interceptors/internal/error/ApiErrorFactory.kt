package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import uk.co.glass_software.android.cache_interceptor.configuration.ErrorFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode.*
import java.io.IOException
import java.util.concurrent.TimeoutException

class ApiErrorFactory : ErrorFactory<ApiError> {

    override fun getError(throwable: Throwable) =
            when (throwable) {
                is IOException,
                is JsonParseException,
                is TimeoutException -> getIoError(throwable)

                is HttpException -> getHttpError(throwable)

                else -> getDefaultError(throwable)
            }

    private fun getHttpError(throwable: HttpException) =
            ApiError(
                    throwable,
                    throwable.code(),
                    parseErrorCode(throwable),
                    throwable.message()
            )

    private fun getIoError(throwable: Throwable) =
            ApiError(
                    throwable,
                    NON_HTTP_STATUS,
                    if (throwable is MalformedJsonException || throwable is JsonParseException) UNEXPECTED_RESPONSE else NETWORK,
                    throwable.message
            )

    private fun getDefaultError(throwable: Throwable) =
            ApiError.from(throwable) ?: ApiError(
                    throwable,
                    NON_HTTP_STATUS,
                    UNKNOWN,
                    "${throwable.javaClass.name}: ${throwable.message}"
            )

    private fun parseErrorCode(httpException: HttpException) =
            when (httpException.code()) {
                401 -> UNAUTHORISED
                404 -> NOT_FOUND
                500 -> SERVER_ERROR
                else -> UNKNOWN
            }
}

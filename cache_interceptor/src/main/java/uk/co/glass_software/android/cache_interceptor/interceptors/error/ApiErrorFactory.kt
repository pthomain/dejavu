package uk.co.glass_software.android.cache_interceptor.interceptors.error

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.*
import java.io.IOException
import java.util.concurrent.TimeoutException

class ApiErrorFactory : (Throwable) -> ApiError {

    override fun invoke(throwable: Throwable) =
            if (throwable is IOException
                    || throwable is JsonParseException
                    || throwable is TimeoutException)
                getIoError(throwable)
            else if (throwable is HttpException)
                getHttpError(throwable)
            else
                ApiError.from(throwable) ?: getError(
                        NON_HTTP_STATUS,
                        UNKNOWN,
                        throwable.javaClass.name
                                + ": "
                                + throwable.message,
                        throwable
                )

    private fun getHttpError(throwable: HttpException) = getError(
            throwable.code(),
            parseErrorCode(throwable),
            throwable.message(),
            throwable
    )

    private fun getIoError(throwable: Throwable) =
            (throwable is MalformedJsonException || throwable is JsonParseException).let {
                getError(
                        NON_HTTP_STATUS,
                        if (it) UNEXPECTED_RESPONSE else NETWORK,
                        throwable.message,
                        throwable
                )
            }

    private fun getError(httpStatus: Int,
                         errorCode: ErrorCode,
                         rawDescription: String?,
                         cause: Throwable) = ApiError(
            cause,
            httpStatus,
            errorCode,
            rawDescription
    )

    private fun parseErrorCode(httpException: HttpException) =
            when (httpException.code()) {
                401 -> UNAUTHORISED
                404 -> NOT_FOUND
                else -> UNKNOWN
            }
}

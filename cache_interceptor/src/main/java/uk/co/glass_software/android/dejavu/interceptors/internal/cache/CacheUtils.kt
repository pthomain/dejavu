package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import uk.co.glass_software.android.boilerplate.utils.io.TAG
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import java.io.Closeable


internal inline fun <T : Closeable?, R> T.useAndLogError(logger: Logger,
                                                         block: (T) -> R) =
        try {
            use(block)
        } catch (e: Exception) {
            logger.e(TAG, "Caught an IO exception")
            throw e
        }

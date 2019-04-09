package uk.co.glass_software.android.dejavu.interceptors.internal

import android.content.Context
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.boilerplate.core.utils.rx.waitForNetwork
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.util.concurrent.TimeUnit.MILLISECONDS

internal fun <E> addConnectivityTimeOutIfNeeded(context: Context,
                                                logger: Logger,
                                                instruction: CacheInstruction,
                                                upstream: Observable<ResponseWrapper<E>>)
        where E : Exception,
              E : NetworkErrorProvider =
        instruction.operation.let {
            if (it is CacheInstruction.Operation.Expiring) {
                val timeOut = it.connectivityTimeoutInMillis ?: 0L
                if (timeOut > 0L)
                    upstream.waitForNetwork(context, logger)
                            .timeout(timeOut, MILLISECONDS)
                else
                    upstream
            } else upstream
        }
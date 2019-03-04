package uk.co.glass_software.android.dejavu.interceptors.internal

import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.utils.rx.waitForNetwork
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.util.concurrent.TimeUnit.MILLISECONDS

internal fun <E> addConnectivityTimeOutIfNeeded(instruction: CacheInstruction,
                                                upstream: Observable<ResponseWrapper<E>>)
        where E : Exception,
              E : NetworkErrorProvider =
        instruction.operation.let {
            if (it is CacheInstruction.Operation.Expiring) {
                val timeOut = it.connectivityTimeoutInMillis ?: 0L
                if (timeOut > 0L)
                    upstream.waitForNetwork().timeout(timeOut, MILLISECONDS)
                else
                    upstream
            } else upstream
        }
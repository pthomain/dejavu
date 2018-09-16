package uk.co.glass_software.android.cache_interceptor.demo

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.lambda.Callback1
import uk.co.glass_software.android.boilerplate.log.CompositeLogger
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.boilerplate.log.Printer
import uk.co.glass_software.android.boilerplate.log.SimpleLogger
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse

abstract class DemoPresenter protected constructor(context: Context,
                                                   onLogOutput: (String) -> Unit) {

    protected val gson = Gson()
    protected val uiLogger = CompositeLogger(
            Boilerplate.init(context, true, "RxCacheLog").let { Boilerplate.logger },
            SimpleLogger(
                    true,
                    object : Printer {
                        override fun canPrint(className: String) = true

                        override fun print(priority: Int,
                                           tag: String?,
                                           message: String) {
                            onLogOutput(clean(message))
                        }
                    }
            ))

    private fun clean(message: String) = message.replace("(\\([^)]+\\))".toRegex(), "").trim()

    internal fun loadResponse(isRefresh: Boolean) =
            getResponseObservable(isRefresh)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

    protected abstract fun getResponseObservable(isRefresh: Boolean): Observable<out JokeResponse>

    abstract fun clearEntries()

    companion object {
        internal const val BASE_URL = "http://api.icndb.com/"
        internal const val ENDPOINT = "jokes/random"
    }

}

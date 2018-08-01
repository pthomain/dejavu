package uk.co.glass_software.android.cache_interceptor.demo

import android.content.Context
import com.facebook.soloader.SoLoader.init

import com.google.gson.Gson

import java.util.concurrent.Callable

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse
import uk.co.glass_software.android.shared_preferences.utils.SimpleLogger

abstract class DemoPresenter protected constructor(onLogOutput: (String) -> Unit) {

    protected val simpleLogger: SimpleLogger
    protected val gson: Gson

    init {
        simpleLogger = SimpleLogger { _, _, message ->
            onLogOutput(clean(message))
        }
        gson = Gson()
    }

    private fun clean(message: String): String {
        return message.replace("(\\([^)]+\\))".toRegex(), "")
    }

    internal fun loadResponse(isRefresh: Boolean): Observable<out JokeResponse> {
        return getResponseObservable(isRefresh)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    protected abstract fun getResponseObservable(isRefresh: Boolean): Observable<out JokeResponse>

    abstract fun clearEntries()

    companion object {
        internal const val BASE_URL = "http://api.icndb.com/"
        internal const val ENDPOINT = "jokes/random"
    }

}

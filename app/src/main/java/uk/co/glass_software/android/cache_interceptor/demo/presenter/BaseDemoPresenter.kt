package uk.co.glass_software.android.cache_interceptor.demo.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.Boilerplate.logger
import uk.co.glass_software.android.boilerplate.ui.mvp.MvpPresenter
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.io
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract.*
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse

internal abstract class BaseDemoPresenter protected constructor(demoActivity: DemoActivity,
                                                                uiLogger: Logger
) : MvpPresenter<DemoMvpView, DemoPresenter, DemoViewComponent>(demoActivity),
        DemoPresenter {

    protected val gson by lazy { Gson() }

    protected val rxCache by lazy {
        RxCache.build(
                CacheConfiguration
                        .builder()
                        .gson(gson)
                        .mergeOnNextOnError(true)
                        .networkTimeOutInSeconds(1)
                        .logger(uiLogger)
                        .build(demoActivity)
        )
    }

    final override fun loadCatFact(isRefresh: Boolean,
                                   encrypt: Boolean,
                                   compress: Boolean,
                                   freshOnly: Boolean) {
        getResponseObservable(
                isRefresh,
                encrypt,
                compress,
                freshOnly
        )
                .io()
                .doOnSubscribe { mvpView.onCallStarted() }
                .doOnComplete(mvpView::onCallComplete)
                .autoSubscribe(mvpView::showCatFact)
    }

    final override fun clearEntries() {
        getClearEntriesCompletable()
                .io()
                .doOnSubscribe { mvpView.onCallStarted() }
                .doOnComplete(mvpView::onCallComplete)
                .autoSubscribe()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDestroy() {
        logger.d("Clearing subscriptions")
        subscriptions.clear()
    }

    protected abstract fun getResponseObservable(isRefresh: Boolean,
                                                 encrypt: Boolean,
                                                 compress: Boolean,
                                                 freshOnly: Boolean)
            : Observable<out CatFactResponse>

    protected abstract fun getClearEntriesCompletable(): Completable

    companion object {
        internal const val BASE_URL = "https://catfact.ninja/"
        internal const val ENDPOINT = "fact"
    }

}


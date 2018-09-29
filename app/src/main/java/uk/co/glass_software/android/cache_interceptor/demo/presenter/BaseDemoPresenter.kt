package uk.co.glass_software.android.cache_interceptor.demo.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.Boilerplate.logger
import uk.co.glass_software.android.boilerplate.ui.mvp.MvpPresenter
import uk.co.glass_software.android.boilerplate.utils.rx.io
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract.*
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse

internal abstract class BaseDemoPresenter protected constructor(demoActivity: DemoActivity)
    : MvpPresenter<DemoMvpView, DemoPresenter, DemoViewComponent>(demoActivity),
        DemoPresenter {

    protected val gson = Gson()

    final override fun loadCatFact(isRefresh: Boolean) {
        getResponseObservable(isRefresh).io()
                .doOnSubscribe { mvpView.onCallStarted() }
                .doOnComplete(mvpView::onCallComplete)
                .autoSubscribe(mvpView::showCatFact)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDestroy() {
        logger.d("Clearing subscriptions")
        subscriptions.clear()
    }

    protected abstract fun getResponseObservable(isRefresh: Boolean): Observable<out CatFactResponse>

    companion object {
        internal const val BASE_URL = "https://catfact.ninja/"
        internal const val ENDPOINT = "fact"
    }

}


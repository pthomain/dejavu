package uk.co.glass_software.android.cache_interceptor.demo

import dagger.Component
import uk.co.glass_software.android.boilerplate.ui.mvp.base.MvpContract.*
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.demo.injection.DemoViewModule
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import javax.inject.Singleton

internal class DemoMvpContract {

    interface DemoMvpView : MvpView<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun showCatFact(response: CatFactResponse)
        fun onCallStarted()
        fun onCallComplete()

    }

    interface DemoPresenter : Presenter<DemoMvpView, DemoPresenter, DemoViewComponent> {

        val configuration: CacheConfiguration<ApiError>

        fun loadCatFact(isRefresh: Boolean,
                        encrypt: Boolean,
                        compress: Boolean,
                        freshOnly: Boolean)

        fun clearEntries()
        fun invalidate()
        fun offline(freshOnly: Boolean)

    }

    @Singleton
    @Component(modules = [DemoViewModule::class])
    interface DemoViewComponent : ViewComponent<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun presenterSwitcher(): Callback1<Method>

    }

}
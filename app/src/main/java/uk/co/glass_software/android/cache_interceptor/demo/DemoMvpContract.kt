package uk.co.glass_software.android.cache_interceptor.demo

import dagger.Component
import uk.co.glass_software.android.boilerplate.ui.mvp.base.MvpContract.*
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.demo.injection.DemoViewModule
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method

internal class DemoMvpContract {

    interface DemoMvpView : MvpView<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun showCatFact(response: CatFactResponse)
        fun onCallStarted()
        fun onCallComplete()

    }

    interface DemoPresenter : Presenter<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun loadCatFact(isRefresh: Boolean,
                        encrypt: Boolean,
                        compress: Boolean,
                        freshOnly: Boolean)

        fun clearEntries()

    }

    @Component(modules = [DemoViewModule::class])
    interface DemoViewComponent : ViewComponent<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun presenterSwitcher(): Callback1<Method>

    }

}
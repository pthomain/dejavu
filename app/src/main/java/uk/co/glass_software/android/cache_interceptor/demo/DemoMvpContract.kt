package uk.co.glass_software.android.cache_interceptor.demo

import dagger.Component
import io.reactivex.Completable
import uk.co.glass_software.android.boilerplate.ui.mvp.base.MvpContract.*
import uk.co.glass_software.android.cache_interceptor.demo.injection.DemoViewModule
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse

internal class DemoMvpContract {

    interface DemoMvpView : MvpView<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun showCatFact(response: CatFactResponse)
        fun onCallStarted()
        fun onCallComplete()

    }

    interface DemoPresenter : Presenter<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun loadCatFact(isRefresh: Boolean)
        fun clearEntries(): Completable

    }

    @Component(modules = [DemoViewModule::class])
    interface DemoViewComponent : ViewComponent<DemoMvpView, DemoPresenter, DemoViewComponent>

}
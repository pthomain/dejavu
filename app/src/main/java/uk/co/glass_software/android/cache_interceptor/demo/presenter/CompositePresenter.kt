package uk.co.glass_software.android.cache_interceptor.demo.presenter

import io.reactivex.disposables.CompositeDisposable
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method.RETROFIT
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.volley.VolleyDemoPresenter

internal class CompositePresenter(override val mvpView: DemoActivity,
                                  private val retrofitDemoPresenter: RetrofitDemoPresenter,
                                  private val volleyDemoPresenter: VolleyDemoPresenter)
    : DemoPresenter, Callback1<Method> {

    override val configuration = getPresenter().configuration
    override var subscriptions = CompositeDisposable()

    enum class Method {
        RETROFIT,
        VOLLEY
    }

    private var method = RETROFIT

    override fun invoke(p1: Method) {
        method = p1
    }

    override fun loadCatFact(isRefresh: Boolean,
                             encrypt: Boolean,
                             compress: Boolean,
                             freshOnly: Boolean) =
            getPresenter().loadCatFact(
                    isRefresh,
                    encrypt,
                    compress,
                    freshOnly
            )

    override fun clearEntries() = getPresenter().clearEntries()

    override fun invalidate() = getPresenter().invalidate()

    override fun offline(freshOnly: Boolean) = getPresenter().offline(freshOnly)

    private fun getPresenter() =
            if (method == RETROFIT) retrofitDemoPresenter
            else volleyDemoPresenter

}
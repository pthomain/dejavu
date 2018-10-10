package uk.co.glass_software.android.cache_interceptor.demo.injection

import dagger.Module
import dagger.Provides
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.boilerplate.utils.log.CompositeLogger
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.log.Printer
import uk.co.glass_software.android.boilerplate.utils.log.SimpleLogger
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.volley.VolleyDemoPresenter
import javax.inject.Named

@Module
internal class DemoViewModule constructor(private val demoActivity: DemoActivity,
                                          private val onLogOutput: (String) -> Unit) {

    @Provides
    @Named("boilerplate")
    fun providerBoilerplateLogger() =
            Boilerplate.init(
                    demoActivity,
                    true,
                    "RxCacheLog"
            ).let { Boilerplate.logger }

    @Provides
    @Named("ui")
    fun providerUiLogger() =
            SimpleLogger(
                    true,
                    object : Printer {
                        override fun canPrint(className: String) = true

                        override fun print(priority: Int,
                                           tag: String?,
                                           message: String) {
                            onLogOutput(clean(message))
                        }

                        private fun clean(message: String) =
                                message.replace(Regex("(\\([^)]+\\))"), "").trim()
                    }
            ) as Logger

    @Provides
    fun providerCompositeLogger(@Named("boilerplate") boilerplateLogger: Logger,
                                @Named("ui") uiLogger: Logger) =
            CompositeLogger(
                    boilerplateLogger,
                    uiLogger
            )

    @Provides
    fun provideRetrofitPresenter(compositeLogger: CompositeLogger) =
            RetrofitDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    fun provideVolleyPresenter(compositeLogger: CompositeLogger) =
            VolleyDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    fun provideCompositePresenter(retrofitDemoPresenter: RetrofitDemoPresenter,
                                  volleyDemoPresenter: VolleyDemoPresenter) =
            CompositePresenter(
                    demoActivity,
                    retrofitDemoPresenter,
                    volleyDemoPresenter
            )

    @Provides
    fun provideDemoPresenter(compositePresenter: CompositePresenter) =
            compositePresenter as DemoMvpContract.DemoPresenter

    @Provides
    fun providePresenterSwitcher(compositePresenter: CompositePresenter) =
            compositePresenter as Callback1<Method>

}
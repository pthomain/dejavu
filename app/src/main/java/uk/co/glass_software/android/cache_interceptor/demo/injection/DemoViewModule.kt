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
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.volley.VolleyDemoPresenter
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class DemoViewModule constructor(private val demoActivity: DemoActivity,
                                          private val onLogOutput: (String) -> Unit) {

    @Provides
    @Singleton
    @Named("boilerplate")
    fun providerBoilerplateLogger() =
            Boilerplate.init(
                    demoActivity,
                    true,
                    "RxCacheLog"
            ).let { Boilerplate.logger }

    @Provides
    @Singleton
    @Named("ui")
    fun providerUiLogger() =
            SimpleLogger(
                    true,
                    object : Printer {
                        override fun canPrint(className: String) = true

                        override fun print(priority: Int,
                                           tag: String?,
                                           message: String) {
                            clean(message).also {
                                if (!it.isNullOrBlank())
                                    onLogOutput(it)
                            }
                        }

                        private fun clean(message: String) =
                                message.replace(Regex("(\\([^)]+\\))"), "")
                                        .replace(Regex("\\n+"), "\n")
                                        .trim()
                    }
            ) as Logger

    @Provides
    @Singleton
    fun providerCompositeLogger(@Named("boilerplate") boilerplateLogger: Logger,
                                @Named("ui") uiLogger: Logger) =
            CompositeLogger(
                    boilerplateLogger,
                    uiLogger
            )

    @Provides
    @Singleton
    fun provideRetrofitAnnotationPresenter(compositeLogger: CompositeLogger) =
            RetrofitAnnotationDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideRetrofitHeaderPresenter(compositeLogger: CompositeLogger) =
            RetrofitHeaderDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideVolleyPresenter(compositeLogger: CompositeLogger) =
            VolleyDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideCompositePresenter(retrofitAnnotationDemoPresenter: RetrofitAnnotationDemoPresenter,
                                  retrofitHeaderDemoPresenter: RetrofitHeaderDemoPresenter,
                                  volleyDemoPresenter: VolleyDemoPresenter) =
            CompositePresenter(
                    demoActivity,
                    retrofitAnnotationDemoPresenter,
                    retrofitHeaderDemoPresenter,
                    volleyDemoPresenter
            )

    @Provides
    @Singleton
    fun provideDemoPresenter(compositePresenter: CompositePresenter) =
            compositePresenter as DemoMvpContract.DemoPresenter

    @Provides
    @Singleton
    fun providePresenterSwitcher(compositePresenter: CompositePresenter) =
            compositePresenter as Callback1<Method>

}
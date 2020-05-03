package dev.pthomain.android.dejavu.demo.dejavu

import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.ObservableClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.SingleClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.adapters.SingleDataClientAdapter
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.adapters.SingleOperationsClientAdapter
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.RetrofitObservableClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.RetrofitSingleClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.RetrofitSingleOperationsClientAdapter
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class DejaVuClient<S : SingleClients.Operations, O : ObservableClients.Operations>(
        private val operationAdapter: (S) -> O
) {

    abstract val dataSingleClient: SingleClients.Data
    abstract val dataObservableClient: ObservableClients.Data
    abstract val operationSingleClient: S
    abstract val operationObservableClient: O

    fun dataClient(useSingle: Boolean) =
            if (useSingle) SingleDataClientAdapter(dataSingleClient)
            else dataObservableClient

    fun operationsClient(useSingle: Boolean) =
            if (useSingle) operationAdapter(operationSingleClient)
            else operationObservableClient
}

class DejaVuRetrofitClient(
        dejaVuRetrofit: DejaVuRetrofit<*>,
        logger: Logger
) : DejaVuClient<RetrofitSingleClients.Operations, RetrofitObservableClients.Operations>(
        ::RetrofitSingleOperationsClientAdapter
) {

    private val retrofit = Retrofit.Builder()
            .baseUrl(BaseDemoPresenter.BASE_URL)
            .client(getOkHttpClient(logger))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .addCallAdapterFactory(dejaVuRetrofit.callAdapterFactory)
            .build()

    override val dataSingleClient =
            retrofit.create(RetrofitSingleClients.Data::class.java)

    override val dataObservableClient =
            retrofit.create(RetrofitObservableClients.Data::class.java)

    override val operationSingleClient =
            retrofit.create(RetrofitSingleClients.Operations::class.java)

    override val operationObservableClient =
            retrofit.create(RetrofitObservableClients.Operations::class.java)

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    logger.d(this, message)
                }
            }).apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
}

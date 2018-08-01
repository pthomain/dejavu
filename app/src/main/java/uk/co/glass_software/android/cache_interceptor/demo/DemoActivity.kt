package uk.co.glass_software.android.cache_interceptor.demo

import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.RadioButton
import android.widget.TextView
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity.Method.RETROFIT
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity.Method.VOLLEY

import uk.co.glass_software.android.cache_interceptor.demo.retrofit.RetrofitDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyDemoPresenter

class DemoActivity : AppCompatActivity() {

    private var method = RETROFIT

    private var loadButton: Button? = null
    private var refreshButton: Button? = null

    private var retrofitDemoPresenter: DemoPresenter? = null
    private var volleyDemoPresenter: DemoPresenter? = null
    private var listAdapter: ExpandableListAdapter? = null

    private val demoPresenter: DemoPresenter?
        get() {
            return when (method) {
                RETROFIT -> retrofitDemoPresenter
                VOLLEY -> volleyDemoPresenter
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadButton = findViewById(R.id.load_button)
        refreshButton = findViewById(R.id.refresh_button)
        loadButton!!.setOnClickListener { ignore -> onButtonClick(false) }
        refreshButton!!.setOnClickListener { ignore -> onButtonClick(true) }

        val clearButton = findViewById<Button>(R.id.clear_button)
        clearButton.setOnClickListener { _ -> demoPresenter!!.clearEntries() }

        findViewById<View>(R.id.github).setOnClickListener { _ -> openGithub() }

        val retrofitRadioButton = findViewById<RadioButton>(R.id.radio_button_retrofit)
        val volleyRadioButton = findViewById<RadioButton>(R.id.radio_button_volley)

        retrofitRadioButton.setOnClickListener { _ -> method = RETROFIT }
        volleyRadioButton.setOnClickListener { _ -> method = VOLLEY }

        val jokeView = findViewById<TextView>(R.id.joke)
        listAdapter = ExpandableListAdapter(
                this,
                { text: CharSequence -> jokeView.text = text },
                { setButtonsEnabled(true) }
        )

        retrofitDemoPresenter = RetrofitDemoPresenter(this) { listAdapter!!.log(it) }
        volleyDemoPresenter = VolleyDemoPresenter(this, { listAdapter!!.log(it) })

        val result = findViewById<ExpandableListView>(R.id.result)
        result.setAdapter(listAdapter)
        listAdapter!!.notifyDataSetChanged()
    }

    private fun onButtonClick(isRefresh: Boolean) {
        setButtonsEnabled(false)
        listAdapter!!.loadJoke(demoPresenter!!.loadResponse(isRefresh))
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        loadButton!!.isEnabled = isEnabled
        refreshButton!!.isEnabled = isEnabled
    }

    private fun openGithub() {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse("https://github.com/pthomain/RxCacheInterceptor"))
    }

    private enum class Method {
        RETROFIT,
        VOLLEY
    }
}

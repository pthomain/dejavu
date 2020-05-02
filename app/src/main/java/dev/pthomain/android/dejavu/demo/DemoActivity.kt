/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.demo

import android.content.Context
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.multidex.MultiDex
import com.uber.rxdogtag.RxDogTag
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.lambda.Callback1
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.ANY
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.FRESH_ONLY
import dev.pthomain.android.dejavu.demo.DemoMvpContract.*
import dev.pthomain.android.dejavu.demo.injection.DemoViewModule
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.CompositePresenter.Method
import dev.pthomain.android.dejavu.demo.presenter.CompositePresenter.Method.RETROFIT_ANNOTATION
import dev.pthomain.android.dejavu.demo.presenter.CompositePresenter.Method.RETROFIT_HEADER
import io.reactivex.plugins.RxJavaPlugins
import org.koin.dsl.koinApplication


internal class DemoActivity
    : AppCompatActivity(),
        DemoMvpView,
        (String) -> Unit {

    private lateinit var listAdapter: ExpandableListAdapter

    private val loadButton by lazy { findViewById<View>(R.id.load_button)!! }
    private val refreshButton by lazy { findViewById<View>(R.id.refresh_button)!! }
    private val clearButton by lazy { findViewById<View>(R.id.clear_button)!! }
    private val offlineButton by lazy { findViewById<View>(R.id.offline_button)!! }
    private val invalidateButton by lazy { findViewById<View>(R.id.invalidate_button)!! }
    private val gitHubButton by lazy { findViewById<View>(R.id.github)!! }

    private val observableRadio by lazy { findViewById<View>(R.id.radio_button_observable)!! }
    private val singleRadio by lazy { findViewById<View>(R.id.radio_button_single)!! }

    private val retrofitAnnotationRadio by lazy { findViewById<View>(R.id.radio_button_retrofit_annotation)!! }
    private val retrofitHeaderRadio by lazy { findViewById<View>(R.id.radio_button_retrofit_header)!! }

    private val connectivityTimeoutCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_connectivity_timeout)!! }
    private val freshOnlyCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_fresh_only)!! }
    private val compressCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_compress)!! }
    private val encryptCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_encrypt)!! }

    private val listView by lazy { findViewById<ExpandableListView>(R.id.list)!! }

    private lateinit var presenter: DemoPresenter
    private lateinit var presenterSwitcher: Callback1<Method>

    override fun getPresenter() = presenter

    override fun initialiseComponent() = DemoViewComponent(
            koinApplication {
                modules(
                        DemoViewModule(
                                this@DemoActivity,
                                this@DemoActivity
                        ).module
                )
            }.koin
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RxDogTag.install()
        onCreateComponent(savedInstanceState)
    }

    override fun onComponentReady(component: DemoViewComponent) {
        this.presenter = component.presenter()
        this.presenterSwitcher = component.presenterSwitcher()
        RxJavaPlugins.setErrorHandler { error ->
            component.logger().e(this, error)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun invoke(p1: String) {
        listAdapter.log(p1)
    }

    override fun onCreateMvpView(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        loadButton.setOnClickListener { presenter.loadCatFact(false) }
        refreshButton.setOnClickListener { presenter.loadCatFact(true) }
        clearButton.setOnClickListener { presenter.clearEntries() }
        offlineButton.setOnClickListener { presenter.offline() }
        invalidateButton.setOnClickListener { presenter.invalidate() }

        observableRadio.setOnClickListener { presenter.useSingle = false }
        singleRadio.setOnClickListener { presenter.useSingle = true }

        retrofitAnnotationRadio.setOnClickListener { presenterSwitcher(RETROFIT_ANNOTATION) }
        retrofitHeaderRadio.setOnClickListener { presenterSwitcher(RETROFIT_HEADER) }
        gitHubButton.setOnClickListener { openGithub() }

        connectivityTimeoutCheckBox.setOnCheckedChangeListener { _, isChecked -> presenter.connectivityTimeoutOn = isChecked }
        freshOnlyCheckBox.setOnCheckedChangeListener { _, isChecked -> presenter.freshness = ifElse(isChecked, FRESH_ONLY, ANY) } //TODO FRESH_PREFERRED
        compressCheckBox.setOnCheckedChangeListener { _, isChecked -> presenter.compress = isChecked }
        encryptCheckBox.setOnCheckedChangeListener { _, isChecked -> presenter.encrypt = isChecked }

        listAdapter = ExpandableListAdapter(this)
        listView.setAdapter(listAdapter)

        listAdapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onInvalidated() {
                onChanged()
            }

            override fun onChanged() {
                for (x in 0 until listAdapter.groupCount) {
                    listView.expandGroup(x)
                }
            }
        })
    }

    override fun showCatFact(response: CatFactResponse) {
        listAdapter.showResponse(response)
    }

    override fun showResult(result: DejaVuResult<CatFactResponse>) {
        listAdapter.showDejaVuResult(result)
    }

    override fun onCallStarted() {
        listView.post {
            setButtonsEnabled(false)
            listAdapter.onStart(
                    presenter.useSingle,
                    presenter.useHeader,
                    presenter.getCacheOperation()
            )
        }
    }

    override fun onCallComplete() {
        listView.post {
            setButtonsEnabled(true)
            listAdapter.onComplete()
        }
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        loadButton.isEnabled = isEnabled
        refreshButton.isEnabled = isEnabled
        clearButton.isEnabled = isEnabled
        invalidateButton.isEnabled = isEnabled
        offlineButton.isEnabled = isEnabled
    }

    private fun openGithub() {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse("https://github.com/pthomain/dejavu"))
    }

    override fun context() = this

}

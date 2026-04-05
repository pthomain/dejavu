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

import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.ANY
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.FRESH_ONLY
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.DemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.DemoPresenter.PersistenceType

internal class DemoActivity : AppCompatActivity(), (String) -> Unit {

    private lateinit var listAdapter: ExpandableListAdapter
    private lateinit var presenter: DemoPresenter

    private val loadButton by lazy { findViewById<View>(R.id.load_button)!! }
    private val refreshButton by lazy { findViewById<View>(R.id.refresh_button)!! }
    private val clearButton by lazy { findViewById<View>(R.id.clear_button)!! }
    private val offlineButton by lazy { findViewById<View>(R.id.offline_button)!! }
    private val invalidateButton by lazy { findViewById<View>(R.id.invalidate_button)!! }
    private val gitHubButton by lazy { findViewById<View>(R.id.github)!! }

    private val retrofitAnnotationRadio by lazy { findViewById<View>(R.id.radio_button_retrofit_annotation)!! }
    private val retrofitHeaderRadio by lazy { findViewById<View>(R.id.radio_button_retrofit_header)!! }

    private val databaseRadio by lazy { findViewById<View>(R.id.radio_button_database)!! }
    private val memoryRadio by lazy { findViewById<View>(R.id.radio_button_memory)!! }

    private val freshOnlyCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_fresh_only)!! }
    private val encryptCheckBox by lazy { findViewById<CheckBox>(R.id.checkbox_encrypt)!! }

    private val listView by lazy { findViewById<ExpandableListView>(R.id.list)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = DemoPresenter(this, this)

        loadButton.setOnClickListener { presenter.loadCatFact(false) }
        refreshButton.setOnClickListener { presenter.loadCatFact(true) }
        clearButton.setOnClickListener { presenter.clearEntries() }
        offlineButton.setOnClickListener { presenter.offline() }
        invalidateButton.setOnClickListener { presenter.invalidate() }

        retrofitAnnotationRadio.setOnClickListener {
            presenter.useAnnotations = true
        }
        retrofitHeaderRadio.setOnClickListener {
            presenter.useAnnotations = false
        }

        databaseRadio.setOnClickListener { presenter.persistence = PersistenceType.SQLITE }
        memoryRadio.setOnClickListener { presenter.persistence = PersistenceType.MEMORY }

        gitHubButton.setOnClickListener { openGithub() }

        freshOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            presenter.freshness = if (isChecked) FRESH_ONLY else ANY
        }
        encryptCheckBox.setOnCheckedChangeListener { _, isChecked ->
            presenter.encrypt = isChecked
        }

        listAdapter = ExpandableListAdapter(this)
        listView.setAdapter(listAdapter)

        listAdapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onInvalidated() = onChanged()
            override fun onChanged() {
                for (x in 0 until listAdapter.groupCount) {
                    listView.expandGroup(x)
                }
            }
        })
    }

    override fun invoke(logLine: String) {
        listAdapter.log(logLine)
    }

    fun showCatFact(response: CatFactResponse) {
        listAdapter.showResponse(response)
    }

    fun showResult(result: DejaVuResult<CatFactResponse>) {
        listAdapter.showDejaVuResult(result)
    }

    fun onCallStarted() {
        listView.post {
            setButtonsEnabled(false)
            listAdapter.onStart(
                    presenter.useAnnotations,
                    presenter.getCacheOperation()
            )
        }
    }

    fun onCallComplete() {
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

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun openGithub() {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse("https://github.com/pthomain/dejavu"))
    }
}

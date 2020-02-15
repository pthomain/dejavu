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
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import java.text.SimpleDateFormat
import java.util.*

internal class ExpandableListAdapter(context: Context)
    : BaseExpandableListAdapter() {

    private val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val simpleDateFormat = SimpleDateFormat("MM/dd/YY hh:mm:ss")

    private val headers: LinkedList<String> = LinkedList()
    private val logs: LinkedList<String> = LinkedList()
    private val children: LinkedHashMap<String, List<*>> = LinkedHashMap()

    private var callStart = 0L

    fun onStart(useSingle: Boolean,
                operation: Operation) {
        headers.clear()
        children.clear()
        logs.clear()

        callStart = System.currentTimeMillis()

        val header = "Retrofit Call"
        headers.add(header)
        children[header] = listOf(Pair(useSingle, operation))

        notifyDataSetChanged()
    }

    fun showCatFact(catFactResponse: CatFactResponse) {
        val cacheToken = catFactResponse.cacheToken
        val exception: Glitch? = null // metadata.exception FIXME this is now handled in onError (merging is gone)
        val operation = cacheToken.instruction.operation
        val callDuration = catFactResponse.callDuration

        val elapsed = "${operation.type.name} -> ${cacheToken.status} (${callDuration.total}ms)"
        val duration = "Call duration: disk = ${callDuration.disk}ms, network = ${callDuration.network}ms, total = ${callDuration.total}ms"

        val info = ArrayList<String>()
        val header: String

        info.add("Cache token instruction: $operation")
        info.add("Cache token status: ${cacheToken.status} (coming from ${getOrigin(cacheToken.status)})")

        if (exception != null) {
            header = "An error occurred: $elapsed"

            info.add("Description: " + exception.description)
            info.add("Message: " + exception.message)
            info.add("Cause: " + exception.cause)
            info.add(duration)
        } else {
            header = elapsed

            cacheToken.cacheDate?.also {
                info.add("Cache token cache date: " + simpleDateFormat.format(it))
            }

            info.add(duration)

            if (operation is Cache) {
                info.add("Cache token expiry date: "
                        + simpleDateFormat.format(cacheToken.expiryDate)
                        + " (TTL: "
                        + operation.durationInSeconds
                        + "s)"
                )
            }
        }

        headers.add(header)
        children[header] = info

        val catFactHeader = "Cat Fact (${catFactResponse.cacheToken.status})"
        headers.add(catFactHeader)
        children[catFactHeader] = listOf(catFactResponse.fact ?: "N/A")

        notifyDataSetChanged()
    }

    fun onComplete() {
        val header = "Log Output (Total: " + (System.currentTimeMillis() - callStart) + "ms)"
        headers.add(header)
        children[header] = logs
        notifyDataSetChanged()
    }

    private fun getOrigin(status: CacheStatus) =
            when (status) {
                INSTRUCTION,
                DONE -> "instruction"
                NOT_CACHED,
                NETWORK,
                REFRESHED -> "network"
                FRESH,
                STALE,
                COULD_NOT_REFRESH -> "disk"
                EMPTY -> "network or disk"
            } + ", " + (if (status.isFinal) "final" else "non-final")

    fun log(output: String) {
        logs.addLast(output)
    }

    override fun getChild(groupPosition: Int,
                          childPosition: Int) = headers[groupPosition].let {
        children[it]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    override fun getGroupView(groupPosition: Int,
                              isExpanded: Boolean,
                              convertView: View?,
                              parent: ViewGroup) =
            (convertView ?: inflater.inflate(R.layout.list_group, parent, false))
                    .apply {
                        findViewById<TextView>(R.id.listHeader).apply {
                            setTypeface(null, Typeface.BOLD)
                            text = getGroup(groupPosition)
                        }
                    }!!

    override fun getChildView(groupPosition: Int,
                              childPosition: Int,
                              isLastChild: Boolean,
                              convertView: View?,
                              parent: ViewGroup): View =
            (convertView ?: inflater.inflate(R.layout.list_item, parent, false))
                    .apply {
                        val child = getChild(groupPosition, childPosition)
                        val text = findViewById<TextView>(R.id.listItem)
                        val instruction = findViewById<InstructionView>(R.id.instruction)

                        if (child is String) {
                            text.visibility = View.VISIBLE
                            instruction.visibility = View.GONE
                            text.text = child
                        } else if (child is Pair<*, *>) {
                            text.visibility = View.GONE
                            instruction.visibility = View.VISIBLE
                            instruction.setOperation(
                                    child.first as Boolean,
                                    child.second as Operation,
                                    CatFactResponse::class.java
                            )
                        }
                    }

    override fun getChildrenCount(groupPosition: Int) = children[headers[groupPosition]]!!.size

    override fun getGroup(groupPosition: Int) = headers[groupPosition]

    override fun getGroupCount() = headers.size

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int,
                                   childPosition: Int) = false
}

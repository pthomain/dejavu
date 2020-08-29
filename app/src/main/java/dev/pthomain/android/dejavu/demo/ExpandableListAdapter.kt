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
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.cache.metadata.response.*
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter.Method
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.glitch.Glitch
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

    fun onStart(
            method: Method,
            useSingle: Boolean,
            operation: Operation
    ) {
        headers.clear()
        children.clear()
        logs.clear()

        callStart = System.currentTimeMillis()

        val header = "Retrofit Call"
        headers.add(header)
        children[header] = listOf(Triple(method, useSingle, operation))

        notifyDataSetChanged()
    }

    @Suppress("UNCHECKED_CAST")
    fun showDejaVuResult(result: DejaVuResult<CatFactResponse>) {
        when (result) {
            is Response<CatFactResponse, *> -> showResponse(result.response)

            is Empty<CatFactResponse, *, *> -> showHeaderAndBody(
                    InternalResult.Empty(result as Empty<CatFactResponse, out Remote, Glitch>),
                    "No response due to filtering or exception"
            )

            is Result<CatFactResponse, *> -> showHeaderAndBody(
                    InternalResult.Result(result as Result<CatFactResponse, out Local>),
                    "This operation does not support responses"
            )
        }
    }

    fun showResponse(response: CatFactResponse) {
        showHeaderAndBody(
                InternalResult.Response(response),
                "The call returned a ${response.cacheToken.status} response"
        )
    }

    private fun showHeaderAndBody(
            internalResult: InternalResult<*, out CacheToken<*, CatFactResponse>>,
            caption: String
    ) {
        val cacheToken = internalResult.cacheToken
        val operation = cacheToken.instruction.operation
        val callDuration = internalResult.callDuration

        val header = "${operation.type.name} -> ${cacheToken.status} (${callDuration.total}ms)"
        val info = ArrayList<String>()

        info.add(caption)
        info.add("Instruction: $operation")
        info.add("Status: ${cacheToken.status} (coming from ${getOrigin(cacheToken.status)})")
        info.add("Duration: disk = ${callDuration.disk}ms, network = ${callDuration.network}ms, total = ${callDuration.total}ms")

        headers.add(header)

        when (internalResult) {
            is InternalResult.Response -> with(internalResult.response.cacheToken) {
                if(operation is Remote.Cache) {
                    info.add("Cache date: " + simpleDateFormat.format(requestDate))
                }
                expiryDate?.also {
                    info.add("Expiry date: "
                            + simpleDateFormat.format(it)
                            + " (TTL: "
                            + (operation as Remote.Cache).durationInSeconds
                            + "s)"
                    )
                }

                val catFactHeader = "Here's a" +
                        " ${ifElse(status.isFresh, "FRESH", "STALE")}" +
                        " cat fact \uD83D\uDE3A" +
                        " (from ${ifElse(status.isFromCache, "cache", "network")})"
                headers.add(catFactHeader)
                children[catFactHeader] = listOf(internalResult.response.fact)
            }

            is InternalResult.Result -> info.add("Operation succeeded")

            is InternalResult.Empty -> with(internalResult.empty) {
                info.add("Description: " + exception.description)
                info.add("Message: " + exception.message)
                info.add("Cause: " + exception.cause)
            }
        }

        children[header] = info

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
                OFFLINE_STALE,
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
                        } else if (child is Triple<*, *, *>) {
                            text.visibility = View.GONE
                            instruction.visibility = View.VISIBLE
                            instruction.setOperation(
                                    child.first as Method,
                                    child.second as Boolean,
                                    child.third as Operation,
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

    private sealed class InternalResult<O : Operation, T : CacheToken<O, CatFactResponse>>(
            delegate: HasMetadata<CatFactResponse, O, T>
    ) : HasMetadata<CatFactResponse, O, T> by delegate {

        class Response(
                val response: CatFactResponse
        ) : InternalResult<Remote, ResponseToken<Remote, CatFactResponse>>(response)

        class Result<O : Local>(
                val result: dev.pthomain.android.dejavu.cache.metadata.response.Result<CatFactResponse, O>
        ) : InternalResult<O, RequestToken<O, CatFactResponse>>(result)

        class Empty<O : Remote>(
                val empty: dev.pthomain.android.dejavu.cache.metadata.response.Empty<CatFactResponse, O, Glitch>
        ) : InternalResult<O, RequestToken<O, CatFactResponse>>(empty)
    }

}

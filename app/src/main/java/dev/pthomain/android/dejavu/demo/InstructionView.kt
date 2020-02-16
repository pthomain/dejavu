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
import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Type
import dev.pthomain.android.dejavu.utils.swapLambdaWhen

class InstructionView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val orange = Color.parseColor("#CC7832")
    private val purple = Color.parseColor("#9876AA")
    private val yellow = Color.parseColor("#FFC66D")
    private val green = Color.parseColor("#629755")
    private val blue = Color.parseColor("#467CDA")
    private val white = Color.parseColor("#A9B7C6")

    fun setOperation(useSingle: Boolean,
                     operation: Operation,
                     responseClass: Class<*>) {
        text = getAnnotationName(operation.type).let {
            TextUtils.concat(
                    getRestMethod(operation),
                    applyOperationStyle(it),
                    getDirectives(it.length + 1, operation),
                    getMethod(useSingle, operation, responseClass)
            )
        }
    }

    private fun getAnnotationName(type: Type) =
            when (type) {
                Type.CACHE -> "@Cache"
                Type.DO_NOT_CACHE -> "@DoNotCache"
                Type.INVALIDATE -> "@Invalidate"
                Type.CLEAR -> "@Clear"
            }

    private fun getRestMethod(operation: Operation) = applyAnnotationStyle(
            ifElse(
                    operation is Remote,
                    "@GET(\"fact\")",
                    "@DELETE(\"fact\")"
            ),
            true
    )

    private fun getDirectives(length: Int,
                              operation: Operation): CharSequence {
        val padding = "".padStart(length, ' ')

        val directives = with(operation) {
            when (this) {
                is Cache -> arrayOf(
                        "priority = ${priority.name}",
                        "durationInSeconds = $durationInSeconds",
                        "connectivityTimeoutInSeconds = $connectivityTimeoutInSeconds",
                        "encrypt = $encrypt",
                        "compress = $compress"
                )

                is Clear -> arrayOf(
                        "clearStaleEntriesOnly = $clearStaleEntriesOnly"
                )

                is Invalidate -> emptyArray()

                else -> emptyArray()
            }
        }

        val styledDirectives = directives.mapIndexed { index, directive ->
            applyDirectiveStyle(
                    (if (index == 0) "" else padding)
                            .plus(directive)
                            .plus((if (index != directives.size - 1) ",\n" else ""))
            )
        }

        val bracketedDirectives = ifElse(
                styledDirectives.isEmpty(),
                "" as CharSequence,
                styledDirectives
        ).let {
            val array = arrayOfNulls<CharSequence>(styledDirectives.size)
            styledDirectives.forEachIndexed { index, charSequence ->
                array[index] = charSequence
            }
            TextUtils.concat("(", TextUtils.concat(*array), ")")
        }!!

        return bracketedDirectives.swapLambdaWhen(bracketedDirectives.isNotEmpty()) {
            applyAnnotationStyle(it!!, false)
        }!!
    }

    private fun applyAnnotationStyle(it: CharSequence,
                                     colourParameters: Boolean): SpannableString {
        val leftBracket = it.indexOf('(')
        val rightBracket = it.indexOf(')')

        return SpannableString(it).apply {
            setSpan(
                    ForegroundColorSpan(orange),
                    0,
                    leftBracket,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                    ForegroundColorSpan(white),
                    leftBracket,
                    leftBracket + 1,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (colourParameters) {
                setSpan(
                        ForegroundColorSpan(green),
                        leftBracket + 1,
                        rightBracket,
                        SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            setSpan(
                    ForegroundColorSpan(white),
                    rightBracket,
                    rightBracket + 1,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                    StyleSpan(BOLD),
                    0,
                    leftBracket,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyOperationStyle(operation: String): SpannableString {
        return SpannableString("\n$operation").apply {
            setSpan(
                    ForegroundColorSpan(orange),
                    0,
                    operation.length + 1,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                    StyleSpan(BOLD),
                    0,
                    operation.length + 1,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyDirectiveStyle(directive: CharSequence): CharSequence {
        val equal = directive.indexOf('=')
        return SpannableString(directive).apply {
            setSpan(
                    ForegroundColorSpan(blue),
                    0,
                    equal + 1,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                    ForegroundColorSpan(purple),
                    equal + 1,
                    directive.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun getMethod(useSingle: Boolean,
                          operation: Operation,
                          responseClass: Class<*>): CharSequence =
            with(operation) {
                when (this) {
                    is Cache -> "${ifElse(
                            priority.freshness.hasSingleResponse,
                            "Single",
                            "Observable"
                    )}<${responseClass.simpleName}>"

                    else -> "CacheOperation<${responseClass.simpleName}>"
                }
            }.let { SpannableString("\nfun call(): $it") }
                    .apply {
                        val leftBracket = indexOf('(')
                        setColourSpan(orange, 0, 4)
                        setColourSpan(yellow, 4, leftBracket)
                        setBoldSpan(0, leftBracket)
                        setColourSpan(white, leftBracket, length)
                    }

    private fun SpannableString.setColourSpan(colour: Int,
                                              start: Int,
                                              end: Int) {
        setSpan(
                ForegroundColorSpan(colour),
                start,
                end,
                SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun SpannableString.setBoldSpan(start: Int,
                                            end: Int) {
        setSpan(
                StyleSpan(BOLD),
                start,
                end,
                SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

}

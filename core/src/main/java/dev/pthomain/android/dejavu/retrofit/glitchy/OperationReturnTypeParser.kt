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

package dev.pthomain.android.dejavu.retrofit.glitchy

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.processor.CacheException
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.type.OutcomeReturnTypeParser.Companion.IsOutcome
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import dev.pthomain.android.glitchy.retrofit.type.ReturnTypeParser
import java.lang.reflect.Type

internal class OperationReturnTypeParser<E>(
        private val dejaVuTypeParser: DejaVuReturnTypeParser<E>,
        private val annotationProcessor: AnnotationProcessor<E>,
        private val logger: Logger
) : ReturnTypeParser<OperationReturnType>
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun parseReturnType(returnType: Type,
                                 annotations: Array<Annotation>): ParsedType<OperationReturnType> {
        val parsedDejaVuType = dejaVuTypeParser.parseReturnType(
                returnType,
                annotations
        )

        val responseClass = parsedDejaVuType.metadata.responseClass

        val methodDescription = "call returning " + getTypedName(
                responseClass,
                parsedDejaVuType.metadata.isDejaVuResult,
                parsedDejaVuType.metadata.isSingle
        )

        val annotationOperation = try {
            annotationProcessor.process(
                    annotations,
                    responseClass
            )
        } catch (cacheException: CacheException) {
            logger.e(
                    this,
                    cacheException,
                    "The annotation on $methodDescription cannot be processed,"
                            + " defaulting to other cache methods if available"
            )
            null
        }

        if (annotationOperation == null) {
            logger.d(
                    this,
                    "Annotation processor for $methodDescription"
                            + " returned no instruction, defaulting to other cache methods if available"
            )
        } else {
            logger.d(
                    this,
                    "Annotation processor for $methodDescription"
                            + " returned the following cache operation "
                            + annotationOperation
            )
        }

        return ParsedType(
                OperationReturnType(
                        annotationOperation,
                        methodDescription,
                        parsedDejaVuType.metadata
                ),
                parsedDejaVuType.returnType,
                parsedDejaVuType.parsedType
        )
    }

    private fun getTypedName(responseClass: Class<*>,
                             isWrapped: Boolean,
                             isSingle: Boolean) =
            String.format(
                    ifElse(isSingle, "Single<%s>", "Observable<%s>"),
                    String.format(
                            ifElse(isWrapped, "DejaVuResult<%s>", "%s"),
                            responseClass.simpleName
                    )
            )
}

internal data class OperationReturnType(
        val annotationOperation: Operation?,
        val methodDescription: String,
        val dejaVuReturnType: DejaVuReturnType
) : IsOutcome

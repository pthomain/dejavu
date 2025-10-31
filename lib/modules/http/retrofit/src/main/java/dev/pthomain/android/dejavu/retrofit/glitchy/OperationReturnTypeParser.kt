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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.CacheException
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.type.OutcomeReturnTypeParser
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import dev.pthomain.android.glitchy.retrofit.type.ReturnTypeParser
import java.lang.reflect.Type

internal class OperationReturnTypeParser<E>(
        private val outcomeReturnTypeParser: OutcomeReturnTypeParser<Class<*>>,
        private val annotationProcessor: AnnotationProcessor,
        private val logger: Logger,
) : ReturnTypeParser<OperationMetadata>
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun parseReturnType(
            returnType: Type,
            annotations: Array<Annotation>,
    ): ParsedType<OperationMetadata> {
        val parsedType = outcomeReturnTypeParser.parseReturnType(
                returnType,
                annotations
        )

        val responseClass = parsedType.wrappedType as Class<*>

        val methodDescription = "call returning " + getTypedName(
                responseClass,
                parsedType
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

        return with(parsedType) {
            ParsedType(
                    OperationMetadata(annotationOperation, methodDescription, this),
                    originalType,
                    rawType,
                    wrappedType,
                    adaptedType
            )
        }
    }

    private fun getTypedName(
            responseClass: Class<*>,
            parsedType: ParsedType<Class<*>>,
    ): String {
        val rawType = (parsedType.rawType as Class<*>).simpleName
        val typeToken = parsedType.typeToken.simpleName

        return "$rawType<$typeToken<${responseClass.simpleName}>>"
    }
}

internal data class OperationMetadata(
        val annotationOperation: Operation?,
        val methodDescription: String,
        val parsedType: ParsedType<Class<*>>,
)

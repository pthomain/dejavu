/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

package dev.pthomain.android.dejavu.retrofit.annotations

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.interceptors.RxType
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Type.*
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException.Type.ANNOTATION

/**
 * Processes Retrofit annotations and generates a CacheInstruction if needed.
 *
 * @see CacheInstruction
 */
internal class AnnotationProcessor<E>(private val logger: Logger)
        where  E : Exception,
               E : NetworkErrorPredicate {

    /**
     * Processes the annotations on the Retrofit call and tries to convert them to a CacheInstruction
     * if applicable.
     *
     * @param annotations the calls annotations as provided by the Retrofit call adapter.
     * @param rxType the type of RxJava operation for this call
     * @param responseClass the target response class
     *
     * @return the processed cache operation if applicable
     */
    @Throws(CacheException::class)
    fun process(annotations: Array<Annotation>,
                rxType: RxType,
                responseClass: Class<*>): Operation? {
        var operation: Operation? = null

        annotations.forEach { annotation ->
            when (annotation) {
                is Cache -> CACHE
                is DoNotCache -> DO_NOT_CACHE
                is Invalidate -> INVALIDATE
                is Clear -> CLEAR
                else -> null
            }?.let {
                operation = getOperation(
                        operation,
                        rxType,
                        responseClass,
                        it,
                        annotation
                )
            }
        }

        return operation
    }

    @Throws(CacheException::class)
    private fun CacheException.logAndThrow() {
        logger.e(this, this)
        throw this
    }

    /**
     * Returns a cache operation for the given annotation and checks for duplicate annotations
     * on the same call.
     *
     * @param currentOperation the previous existing annotation if found, to detect duplicates
     * @param rxType the RxJava type
     * @param responseClass the targeted response class for this call
     * @param foundOperation the operation type associated with the given annotation
     * @param annotation the annotation being processed
     * @return the processed cache operation for the given annotation
     */
    @Throws(CacheException::class)
    private fun getOperation(currentOperation: Operation?,
                             rxType: RxType,
                             responseClass: Class<*>,
                             foundOperation: Operation.Type,
                             annotation: Annotation): Operation? {
        if (currentOperation != null) {
            CacheException(
                    ANNOTATION,
                    "More than one cache annotation defined for method returning"
                            + " ${rxType.getTypedName(responseClass)}, found ${getAnnotationName(foundOperation)}"
                            + " after existing annotation ${getAnnotationName(currentOperation.type)}."
                            + " Only one annotation can be used for this method."
            ).logAndThrow()
        }

        return with(annotation) {
            when (this) {
                is Cache -> Operation.Remote.Cache(
                        priority,
                        durationInSeconds,
                        connectivityTimeoutInSeconds,
                        requestTimeOutInSeconds,
                        encrypt,
                        compress
                )

                is Invalidate -> Operation.Local.Invalidate(useRequestParameters)

                is Clear -> Operation.Local.Clear(
                        clearStaleEntriesOnly,
                        useRequestParameters
                )

                is DoNotCache -> Operation.Remote.DoNotCache

                else -> null
            }
        }
    }

    /**
     * Converts the operation type into a String annotation
     *
     * @param type the operation's type.
     * @return the String representation of the type's annotation
     */
    private fun getAnnotationName(type: Operation.Type) =
            when (type) {
                CACHE -> "@Cache"
                DO_NOT_CACHE -> "@DoNotCache"
                INVALIDATE -> "@Invalidate"
                CLEAR -> "@Clear"
            }

}

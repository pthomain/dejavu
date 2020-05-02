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

package dev.pthomain.android.dejavu.retrofit.annotations.processor

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.CacheException
import dev.pthomain.android.dejavu.cache.CacheException.Type.ANNOTATION
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Type.*
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.DoNotCache
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate

/**
 * Processes Retrofit annotations and generates a CacheInstruction if needed.
 *
 * @see CacheInstruction
 */
internal class AnnotationProcessor<E>(private val logger: Logger)
        where  E : Throwable,
               E : NetworkErrorPredicate {

    /**
     * Processes the annotations on the Retrofit call and tries to convert them to a CacheInstruction
     * if applicable.
     *
     * @param annotations the calls annotations as provided by the Retrofit call adapter.
     * @param responseClass the target response class
     *
     * @return the processed cache operation if applicable
     */
    @Throws(CacheException::class)
    fun process(annotations: Array<Annotation>,
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
     * @param responseClass the targeted response class for this call
     * @param foundOperation the operation type associated with the given annotation
     * @param annotation the annotation being processed
     * @return the processed cache operation for the given annotation
     */
    @Throws(CacheException::class)
    private fun getOperation(currentOperation: Operation?,
                             responseClass: Class<*>,
                             foundOperation: Type,
                             annotation: Annotation): Operation? {
        if (currentOperation != null) {
            CacheException(
                    ANNOTATION,
                    "More than one cache annotation defined for method returning"
                            + " ${responseClass.name}, found ${getAnnotationName(foundOperation)}"
                            + " after existing annotation ${getAnnotationName(currentOperation.type)}."
                            + " Only one annotation can be used for this method."
            ).logAndThrow()
        }

        return with(annotation) {
            when (this) {
                is Cache -> Remote.Cache(
                        priority,
                        durationInSeconds,
                        connectivityTimeoutInSeconds,
                        requestTimeOutInSeconds,
                        encrypt,
                        compress
                )

                is DoNotCache -> Remote.DoNotCache
                is Invalidate -> Local.Invalidate
                is Clear -> Local.Clear(clearStaleEntriesOnly)
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
    private fun getAnnotationName(type: Type) =
            when (type) {
                CACHE -> "@Cache"
                DO_NOT_CACHE -> "@DoNotCache"
                INVALIDATE -> "@Invalidate"
                CLEAR -> "@Clear"
            }

}

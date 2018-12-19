/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.test


import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import uk.co.glass_software.android.boilerplate.utils.lambda.Action

object TestUtils {

    /**
     * Convenience method used to expect exceptions on calls. This is for RxJava calls since the
     * exception mechanism is delivered onError. For calls to Observable.blockingFirst(), the exception
     * is thrown as a RuntimeException.
     *
     * @param exceptionType the exception expected to be called during the call
     * @param message       the expected message of the exception
     * @param `action`          the method to be called
     * @param <E>           the type of the inferred exception
    </E> */
    fun <E> expectException(exceptionType: Class<E>,
                            message: String,
                            action: Action) {
        expectException(exceptionType, message, action, false)
    }

    fun <E> expectExceptionCause(exceptionType: Class<E>,
                                 message: String,
                                 action: Action) {
        expectException(exceptionType, message, action, true)
    }

    private fun <E> expectException(exceptionType: Class<E>,
                                    message: String,
                                    action: Action,
                                    checkCause: Boolean) {
        try {
            action.invoke()
        } catch (e: Exception) {
            val toCheck = if (checkCause) e.cause else e

            if (toCheck != null && exceptionType == toCheck.javaClass) {
                assertEquals("The exception did not have the right message",
                        message,
                        toCheck.message
                )
                return
            }
        }

        fail("Expected exception was not caught: $exceptionType")
    }

}

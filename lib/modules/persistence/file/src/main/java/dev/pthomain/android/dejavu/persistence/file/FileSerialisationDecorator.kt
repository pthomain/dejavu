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

package dev.pthomain.android.dejavu.persistence.file

import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationException
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache

/**
 * Optional file metadata step of the serialisation process (only affecting file persistence).
 *
 * @param byteToStringConverter an factory converting bytes to Strings
 */
internal class FileSerialisationDecorator(
        private val byteToStringConverter: (ByteArray) -> String
) : SerialisationDecorator {

    /**
     * Adds file metadata during the serialisation process.
     *
     * @param responseWrapper the wrapper associated with the payload being serialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the payload with added metadata
     * @throws SerialisationException in case this step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateSerialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ) =
            responseClass.name
                    .plus("\n")
                    .plus(byteToStringConverter(payload))
                    .toByteArray()

    /**
     * Removes file metadata during the deserialisation process.
     *
     * @param instructionToken the request's instruction token associated with the payload being deserialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the payload minus the embedded metadata
     * @throws SerialisationException in case this step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateDeserialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ) =
            byteToStringConverter(payload).let {
                if (it.contains("\n")) it.substring(it.indexOf("\n"))
                else throw SerialisationException("Could not extract the payload")
            }.toByteArray()

}

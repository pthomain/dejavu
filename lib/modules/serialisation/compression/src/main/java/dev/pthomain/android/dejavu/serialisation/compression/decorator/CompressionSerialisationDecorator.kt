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

package dev.pthomain.android.dejavu.serialisation.compression.decorator

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationException
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache

/**
 * Optional compression step of the serialisation process
 *
 * @param logger a Logger instance
 * @param compresser a function compressing an input ByteArray
 * @param uncompresser a function uncompressing an input ByteArray
 */
internal class CompressionSerialisationDecorator(
        private val logger: Logger,
        private val compresser: (ByteArray) -> ByteArray,
        private val uncompresser: (ByteArray) -> ByteArray
) : SerialisationDecorator {

    /**
     * Implements optional compression during the serialisation process.
     *
     * @param responseWrapper the wrapper associated with the payload being serialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the compressed payload
     * @throws SerialisationException in case this compression step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateSerialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ) =
            if (operation.compress) {
                compresser(payload).also { compressed ->
                    logCompression(
                            compressed,
                            responseClass.simpleName,
                            payload
                    )
                }
            } else payload

    /**
     * Implements optional uncompression during the deserialisation process.
     *
     * @param instructionToken the request's instruction token associated with the payload being deserialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the uncompressed payload
     * @throws SerialisationException in case this uncompression step failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> decorateDeserialisation(
            responseClass: Class<R>,
            operation: Cache,
            payload: ByteArray
    ) =
            if (operation.compress) {
                uncompresser(payload).also {
                    logCompression(
                            payload,
                            responseClass.simpleName,
                            it
                    )
                }
            } else payload

    /**
     * Logs the compression level for the given compressed payload
     *
     * @param compressedData the compressed payload
     * @param simpleName the class name of the response associated with the payload
     * @param uncompressed the original payload
     */
    private fun logCompression(
            compressedData: ByteArray,
            simpleName: String,
            uncompressed: ByteArray
    ) {
        logger.d(
                this,
                "Compressed/uncompressed $simpleName: ${compressedData.size}B/${uncompressed.size}B "
                        + "(${100 * compressedData.size / uncompressed.size}%)"
        )
    }
}
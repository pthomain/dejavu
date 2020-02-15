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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate

/**
 * Interface representing a step in the serialisation process provided as a list to the
 * SerialisationManager to be executed in their defined order during serialisation
 * (and in reverse during deserialisation).
 */
interface SerialisationDecorator<E> where E : Exception,
                                          E : NetworkErrorPredicate {

    /**
     * Implements a single concern during the serialisation process.
     *
     * @param responseWrapper the wrapper associated with the payload being serialised
     * @param metadata the overall metadata associated with the current serialisation
     * @param payload the payload being serialised
     * @return the payload converted according to this class' serialisation concern
     * @throws SerialisationException in case this serialisation step failed
     */
    @Throws(SerialisationException::class)
    fun decorateSerialisation(responseWrapper: ResponseWrapper<Cache, RequestToken<Cache>, E>,
                              metadata: SerialisationDecorationMetadata,
                              payload: ByteArray): ByteArray

    /**
     * Implements a single concern during the deserialisation process.
     *
     * @param instructionToken the request's instruction token associated with the payload being deserialised
     * @param metadata the overall metadata associated with the current deserialisation
     * @param payload the payload being deserialised
     * @return the payload converted according to this class' deserialisation concern
     * @throws SerialisationException in case this deserialisation step failed
     */
    @Throws(SerialisationException::class)
    fun decorateDeserialisation(instructionToken: InstructionToken<Cache>,
                                metadata: SerialisationDecorationMetadata,
                                payload: ByteArray): ByteArray

}

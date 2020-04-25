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

package dev.pthomain.android.dejavu.builders.glitch

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.builders.glitch.di.DaggerGlitchDejaVuComponent
import dev.pthomain.android.dejavu.builders.glitch.di.GlitchDejaVuModule
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.configuration.ConfigurationBuilder
import dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.Serialiser
import dev.pthomain.android.dejavu.shared.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory

class DejaVuGlitchBuilder(
        errorFactory: ErrorFactory<Glitch> = DejaVuGlitchFactory(GlitchFactory())
) : ConfigurationBuilder<Glitch>() {

    init {
        withErrorFactory(errorFactory)
    }

    override fun componentProvider(
            context: Context,
            logger: Logger,
            errorFactory: ErrorFactory<Glitch>,
            serialiser: Serialiser,
            persistenceManager: PersistenceManager,
            operationPredicate: (metadata: RequestMetadata<*>) -> Operation.Remote?,
            durationPredicate: (TransientResponse<*>) -> Int?
    ): DejaVuComponent<Glitch> =
            DaggerGlitchDejaVuComponent.builder()
                    .glitchDejaVuModule(GlitchDejaVuModule(
                            context,
                            errorFactory,
                            operationPredicate,
                            durationPredicate
                    ))
                    .build()

}
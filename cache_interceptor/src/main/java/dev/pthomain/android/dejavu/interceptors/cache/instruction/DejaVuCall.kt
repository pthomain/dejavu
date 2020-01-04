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

package dev.pthomain.android.dejavu.interceptors.cache.instruction

import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Observer

/**
 * Observable wrapper capturing the target response type for calls using operations that might not
 * necessarily emit any instance of it.
 *
 * Calls that do not emit any
 * This is useful primarily for operations that would not return a result (i.e. Clear or Invalidate)
 * and can be considered Completable operations but still require information about the target type
 * (which cannot be provided by a Completable).
 *
 * Operations can either be Remote or Local:
 * - Remote operations will return data (either from the API or from the local cache).
 * - Local operations only apply to the existing cache and do not return any data (e.g. Clear or Invalidate).
 *
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Remote
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Local
 *
 * As a result, there are some restrictions as to which return types can be used for those operations.
 *
 *
 *
 * Another use for it is as a return type for any call that defines operations uniquely via header.
 * Since a header might represent an operation that returns data or might not (i.e completable
 * operations), there needs to be a way to handle both cases.
 *
 * By extension, any call can use DejaVuCall which will emit a ResponseWrapper, which will either
 * contain a response of the given target type or an exception in the metadata if no response is
 * available. For completable operations of type Clear or Invalidate, the exception for a successful
 * execution (i.e. returning no response) would be a DoneException.
 *
 * @see dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.DoneException
 */

// TODO DejaVuCall.Action and DejaVuCall.Directive. Header supports both but cache predicate only supports Directive.


/**
 *
 *
 * Annotation: static
 * Header: dynamic
 * Predicate: dynamic
 *
 * With Annotations using an Observable with a completable operation is a coding error and an exception should be thrown.
 * Only a DejavuCall or a Completable is supported in these cases.
 *
 * With Header, completable operation should only be available if the call using DejavuCall or Completable, otherwise throw an exception.
 * The predicate does not support completable operations as they should be deterministic and not reactive.
 *
 *
 */

interface Wrappable<R> : ObservableSource<ResponseWrapper<*>> {
    val originalExceptionClass: Class<*>
}

internal class Wrapped<R, E>(
        private val observable: Observable<ResponseWrapper<*>>,
        override val originalExceptionClass: Class<E>
) : Wrappable<R>
        where E : Exception,
              E : NetworkErrorPredicate {
    override fun subscribe(observer: Observer<in ResponseWrapper<*>>) {
        observable.subscribe(observer)
    }
}



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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import com.google.common.net.HttpHeaders.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.BaseIntegrationTest
import dev.pthomain.android.dejavu.test.assertNotNullWithContext
import dev.pthomain.android.dejavu.test.assertNullWithContext
import dev.pthomain.android.dejavu.test.assertTrueWithContext
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.network.model.User
import org.junit.Test
import java.util.*

//TODO memory
internal abstract class BasePersistenceManagerIntegrationTest<T : PersistenceManager<Glitch>>(
        targetExtractor: (PersistenceManagerFactory<Glitch>) -> T
) : BaseIntegrationTest<PersistenceManager<Glitch>>({ targetExtractor(it.persistenceManagerFactory()) }) {

    @Test
    fun `GIVEN that a response is not cached THEN it should not be returned`() {
        val cachedResponse = target.getCachedResponse(instructionToken())

        assertNullWithContext(
                cachedResponse,
                "Response should be null"
        )
    }

    @Test
    fun `GIVEN that a response is cached THEN it should be returned`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken())

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )
    }

    @Test
    fun `GIVEN that a response has expired THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse(instructionToken(
                Cache(durationInSeconds = -1)
        ))

        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                STALE,
                expiryDate = Date(NOW.time - 1L)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is INVALIDATE THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()
        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null)
        target.invalidate(instructionToken)

        val cachedResponse = target.getCachedResponse(instructionToken)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is REFRESH THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken())

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )

        val refreshToken = instructionToken(Cache(REFRESH))
        val refreshResponse = target.getCachedResponse(refreshToken)

        assertResponse(
                getStubbedTestResponse(refreshToken),
                refreshResponse,
                STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is not REFRESH or INVALIDATE THEN it should have a CACHED status`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken())

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 2 different responses are cached AND the cache is cleared for one type THEN this type should not be returned AND the other type should be returned`() {
        val stubbedResponse = getStubbedTestResponse()
        val userResponse = getStubbedUserResponseWrapper()

        val (testResponseToken, userResponseToken) = cacheTwoResponses(stubbedResponse, userResponse)

        target.clearCache(instructionToken(
                Clear(),
                TestResponse::class.java
        ))

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken)

        assertNullWithContext(
                cachedTestResponseAfterClear,
                "Test response should have been cleared"
        )

        assertResponse(
                userResponse,
                cachedUserResponseAfterClear,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 2 different responses are cached AND the whole cache is cleared THEN both type should not be returned`() {
        val stubbedResponse = getStubbedTestResponse()
        val userResponse = getStubbedUserResponseWrapper()

        val (testResponseToken, userResponseToken) = cacheTwoResponses(stubbedResponse, userResponse)

        target.clearCache(instructionToken(
                Clear(),
                Any::class.java
        ))

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken)

        assertNullWithContext(
                cachedTestResponseAfterClear,
                "Test response should have been cleared"
        )

        assertNullWithContext(
                cachedUserResponseAfterClear,
                "User response should have been cleared"
        )
    }

    @Test
    fun `GIVEN that 2 responses of the same type are cached AND their respective URLs are different THEN both type should be returned independently`() {
        val firstResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val secondResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse2"
        )).let {
            it.copy(
                    response = TestResponse().apply { addAll((it.response as TestResponse).take(5)) }
            )
        }

        val (firstResponseToken, secondResponseToken) = cacheTwoResponses(firstResponse, secondResponse)

        assertTrueWithContext(
                firstResponseToken.instruction.requestMetadata != secondResponseToken.instruction.requestMetadata,
                "Request metadata for 2 responses of the same type with different URLs should be different"
        )

        val cachedFirstResponse = target.getCachedResponse(firstResponseToken)
        val cachedSecondResponse = target.getCachedResponse(secondResponseToken)

        assertNotNullWithContext(
                cachedFirstResponse,
                "Test response should have been cached"
        )

        assertNotNullWithContext(
                cachedSecondResponse,
                "Test response should have been cached"
        )

        assertTrueWithContext(
                cachedFirstResponse!!.response != cachedSecondResponse!!.response,
                "Responses should have a different number of users"
        )
    }

    private fun cacheTwoResponses(
            firstResponse: ResponseWrapper<*, *, Glitch>,
            secondResponse: ResponseWrapper<*, *, Glitch>,
            firstResponseExpectedStatus: CacheStatus = FRESH,
            secondResponseExpectedStatus: CacheStatus = FRESH
    ): Pair<InstructionToken, InstructionToken> {
        val firstResponseToken = firstResponse.metadata.cacheToken
        val secondResponseToken = secondResponse.metadata.cacheToken

        target.cache(firstResponse, null)
        target.cache(secondResponse, null)

        val cachedFirstResponse = target.getCachedResponse(firstResponseToken)
        val cachedSecondResponse = target.getCachedResponse(secondResponseToken)

        assertResponse(
                firstResponse,
                cachedFirstResponse,
                firstResponseExpectedStatus
        )

        assertResponse(
                secondResponse,
                cachedSecondResponse,
                secondResponseExpectedStatus
        )

        return firstResponseToken to secondResponseToken
    }

    @Test
    fun `GIVEN that 2 responses of the same type are cached AND only one has expired AND the cache is cleared for older entries of that type THEN only the expired one should be cleared AND the other one should be returned`() {
        val freshStubbedResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val expiredStubbedResponse = freshStubbedResponse.copy(
                metadata = freshStubbedResponse.metadata.copy(
                        cacheToken = instructionToken(
                                Cache(durationInSeconds = -1),
                                url = "http://test.com/testResponse2"
                        )
                )
        )

        val (freshResponseToken, expiredResponseToken) = cacheTwoResponses(
                freshStubbedResponse,
                expiredStubbedResponse,
                secondResponseExpectedStatus = STALE
        )

        target.clearCache(instructionToken(
                Clear(clearStaleEntriesOnly = true),
                TestResponse::class.java
        ))

        val freshTestResponseAfterClear = target.getCachedResponse(freshResponseToken)
        val expiredUserResponseAfterClear = target.getCachedResponse(expiredResponseToken)

        assertNullWithContext(
                expiredUserResponseAfterClear,
                "Expired TestResponse should have been cleared"
        )

        assertResponse(
                freshStubbedResponse,
                freshTestResponseAfterClear,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 4 responses of 2 different types are cached AND only one of each type has expired AND the whole cache is cleared for older entries THEN only the expired ones should be cleared`() {
        val testResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val userResponse = getStubbedUserResponseWrapper(
                url = "http://test.com/userResponse1",
                instructionToken = instructionToken(responseClass = User::class.java)
        )

        val expiredTestResponse = getStubbedTestResponse(instructionToken(
                Cache(durationInSeconds = -1),
                url = "http://test.com/testResponse2"
        ))

        val expiredUserResponse = getStubbedUserResponseWrapper(
                instructionToken(
                        Cache(durationInSeconds = -1),
                        url = "http://test.com/userResponse2",
                        responseClass = User::class.java
                ),
                url = "http://test.com/userResponse2"
        )

        val (testResponseToken, userResponseToken) = cacheTwoResponses(testResponse, userResponse)
        val (expiredTestResponseToken, expiredUserResponseToken) = cacheTwoResponses(
                expiredTestResponse,
                expiredUserResponse,
                STALE,
                STALE
        )

        val cachedTestResponse = target.getCachedResponse(testResponseToken)
        val cachedUserResponse = target.getCachedResponse(userResponseToken)

        val cachedExpiredTestResponse = target.getCachedResponse(expiredTestResponseToken)
        val cachedExpiredUserResponse = target.getCachedResponse(expiredUserResponseToken)

        assertResponse(
                testResponse,
                cachedTestResponse,
                FRESH
        )

        assertResponse(
                userResponse,
                cachedUserResponse,
                FRESH
        )

        assertResponse(
                testResponse.copy(metadata = testResponse.metadata.copy(cacheToken = instructionToken(
                        Cache(durationInSeconds = -1),
                        url = "http://test.com/testResponse2"
                ))),
                cachedExpiredTestResponse,
                STALE
        )

        assertResponse(
                userResponse.copy(metadata = userResponse.metadata.copy(cacheToken = instructionToken(
                        Cache(durationInSeconds = -1),
                        responseClass = User::class.java,
                        url = "http://test.com/userResponse2"
                ))),
                cachedExpiredUserResponse,
                STALE
        )

        target.clearCache(instructionToken(
                Clear(clearStaleEntriesOnly = true),
                Any::class.java //TODO use Any for clearing all classes
        ))

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken)

        val cachedExpiredTestResponseAfterClear = target.getCachedResponse(expiredTestResponseToken)
        val cachedExpiredUserResponseAfterClear = target.getCachedResponse(expiredUserResponseToken)

        assertResponse(
                testResponse,
                cachedTestResponseAfterClear,
                FRESH
        )

        assertResponse(
                userResponse,
                cachedUserResponseAfterClear,
                FRESH
        )

        assertNullWithContext(
                cachedExpiredTestResponseAfterClear,
                "Expired response should have been cleared"
        )

        assertNullWithContext(
                cachedExpiredUserResponseAfterClear,
                "Expired response should have been cleared"
        )
    }
}

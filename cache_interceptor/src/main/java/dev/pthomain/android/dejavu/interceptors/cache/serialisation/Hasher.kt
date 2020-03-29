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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation

import android.net.Uri
import androidx.annotation.VisibleForTesting
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.interceptors.cache.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.InvalidRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.ValidRequestMetadata
import java.io.UnsupportedEncodingException
import java.security.MessageDigest

/**
 * Handles the hashing of the request's URL and parameters to provide a unique key per distinct request.
 *
 * @param logger a Logger instance
 * @param messageDigest the hashing algorithm
 * @param uriParser a factory converting String to Uri
 */
internal class Hasher(
        private val logger: Logger,
        private val messageDigest: MessageDigest?,
        private val uriParser: (String) -> Uri
) {

    /**
     * Hashes a RequestMetadata
     *
     * @param requestMetadata the plain metadata
     * @return the hashed metadata or null if the hashing of the URL or class name failed.
     */
    fun <R : Any> hash(requestMetadata: PlainRequestMetadata<R>): HashedRequestMetadata<R> {
        val uri = uriParser(requestMetadata.url)
        val sortedParameters = getSortedParameters(uri)

        val sortedUrl = with(uri) {
            "$scheme:$host$path?$sortedParameters"
        }

        val urlAndBody = requestMetadata.requestBody?.let { "$sortedUrl||$it" } ?: sortedUrl

        val urlHash = tryOrNull(this, logger, "Could not hash URL and body") {
            hash(urlAndBody)
        }

        val classHash = tryOrNull(this, logger, "Could not hash response class") {
            hash(requestMetadata.responseClass.name)
        }

        return if (urlHash == null || classHash == null)
            InvalidRequestMetadata(requestMetadata.responseClass)
        else
            ValidRequestMetadata(
                    requestMetadata.responseClass,
                    requestMetadata.url,
                    requestMetadata.requestBody,
                    urlHash,
                    classHash
            )
    }

    /**
     * Sorts the parameters in alphabetical order.
     *
     * @param uri the URI to sort
     * @return a String representing the parameters sorted in alphabetical order
     */
    @VisibleForTesting
    fun getSortedParameters(uri: Uri) =
            uri.queryParameterNames
                    .sorted()
                    .joinToString(separator = "&") {
                        "$it=${uri.getQueryParameter(it)}"
                    }

    /**
     * Hashes a String.
     *
     * @param text the String to hash
     * @return the hashed String
     */
    @Throws(UnsupportedEncodingException::class)
    fun hash(text: String) =
            if (messageDigest == null) {
                var hash: Long = 7
                for (element in text) {
                    hash = hash * 31 + element.toLong()
                }
                hash.toString()
            } else {
                val textBytes = text.toByteArray(charset("UTF-8"))
                messageDigest.update(textBytes, 0, textBytes.size)
                bytesToString(messageDigest.digest())
            }

    /**
     * Serialises a byte array to hexadecimal.
     *
     * @param bytes the array to serialise
     * @return the hexadecimal representation of this array
     */
    private fun bytesToString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Factory providing an instance of Hasher with the available Hashing algorithm.
     *
     * @param logger a Logger instance
     * @param uriParser a factory converting String to Uri
     */
    class Factory(private val logger: Logger,
                  private val uriParser: (String) -> Uri) {

        fun create(): Hasher {
            var messageDigest = tryOrNull(
                    this,
                    logger,
                    "Could not create a SHA-1 message digest"
            ) {
                MessageDigest.getInstance("SHA-1").also {
                    logger.d(this, "Using SHA-1 hasher")
                }
            }

            if (messageDigest == null) {
                messageDigest = tryOrNull(
                        this,
                        logger,
                        "Could not create a MD5 message digest"
                ) {
                    MessageDigest.getInstance("MD5").also {
                        logger.d(this, "Using MD5 hasher")
                    }
                }
            }

            return Hasher(
                    logger,
                    messageDigest,
                    uriParser
            )
        }
    }

    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()
    }
}

private inline fun <T> tryOrNull(caller: Any,
                                 logger: Logger,
                                 message: String,
                                 action: () -> T): T? =
        try {
            action()
        } catch (e: Exception) {
            logger.e(caller, e, message)
            null
        }

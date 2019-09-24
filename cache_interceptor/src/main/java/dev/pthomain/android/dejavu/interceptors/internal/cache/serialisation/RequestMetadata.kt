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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation

//TODO JavaDoc
sealed class RequestMetadata(val responseClass: Class<*>,
                             val url: String,
                             val requestBody: String? = null) {

    class UnHashed(responseClass: Class<*>,
                   url: String,
                   requestBody: String? = null)
        : RequestMetadata(
            responseClass,
            url,
            requestBody
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RequestMetadata

            if (responseClass != other.responseClass) return false
            if (url != other.url) return false
            if (requestBody != other.requestBody) return false

            return true
        }

        override fun hashCode(): Int {
            var result = responseClass.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + (requestBody?.hashCode() ?: 0)
            return result
        }

        override fun toString() =
                "UnHashed(responseClass='${responseClass.name}', url='$url', requestBody=$requestBody)"
    }

    class Hashed internal constructor(responseClass: Class<*>,
                                      url: String,
                                      requestBody: String?,
                                      val urlHash: String,
                                      val classHash: String)
        : RequestMetadata(
            responseClass,
            url,
            requestBody
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Hashed

            if (responseClass != other.responseClass) return false
            if (url != other.url) return false
            if (requestBody != other.requestBody) return false
            if (urlHash != other.urlHash) return false
            if (classHash != other.classHash) return false

            return true
        }

        override fun hashCode(): Int {
            var result = responseClass.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + (requestBody?.hashCode() ?: 0)
            result = 31 * result + urlHash.hashCode()
            result = 31 * result + classHash.hashCode()
            return result
        }

        override fun toString() =
                "Hashed(responseClass='${responseClass.name}', url='$url', requestBody=$requestBody, urlHash=$urlHash, classHash=$classHash)"
    }
}
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

package dev.pthomain.android.dejavu.test.network.model

class Address {

    var street: String? = null
    var suite: String? = null
    var city: String? = null
    var zipcode: String? = null
    var geo: Geo? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Address

        if (street != other.street) return false
        if (suite != other.suite) return false
        if (city != other.city) return false
        if (zipcode != other.zipcode) return false
        if (geo != other.geo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = street?.hashCode() ?: 0
        result = 31 * result + (suite?.hashCode() ?: 0)
        result = 31 * result + (city?.hashCode() ?: 0)
        result = 31 * result + (zipcode?.hashCode() ?: 0)
        result = 31 * result + (geo?.hashCode() ?: 0)
        return result
    }

}
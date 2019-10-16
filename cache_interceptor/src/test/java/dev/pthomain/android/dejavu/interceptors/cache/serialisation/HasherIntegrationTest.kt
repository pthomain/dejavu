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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation


import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.injection.integration.component.IntegrationDejaVuComponent
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.test.BaseIntegrationTest
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest
import java.util.*

@RunWith(RobolectricTestRunner::class)
internal class HasherIntegrationTest : BaseIntegrationTest<Hasher>(IntegrationDejaVuComponent::hasher) {

    private val paramsKeys = "bedac"
    private val paramsKeysInOrder = "abcde"

    private val strings = listOf(
            "http://6qIyrDOprTaxiSa7yH7PbCIOGoji0eZWO9sw.com",
            "http://FWFu3SO1CofJsmF8JEv5KmEb38Jmao9X4MPR.com",
            "http://5FpLfvFcAsjRONmc1gYYfURd82x44naN9gL2.com",
            "http://pRJPTyTJ5jGJscGZzKE8TQQAWFjV3QBfPc7J.com",
            "http://7gSFjcIOsh4u0YItujMqqWlrdmm2yvChqRgq.com",
            "http://SZ7mCBpBTFYZldhqkG3164vb8SnAYNfVtmdU.com",
            "http://pN0oYI9U48nBOCiHO4rkghe2DYPZmhq0RxTd.com",
            "http://MIkRdMIBSihvuui7Z19DoMZ71nlZHKq42FBi.com",
            "http://OHxwhDSBRhShVjkgZACuuF8O44Y1JqJ9ygyb.com",
            "http://CdJBJ7027bLwJEOD81DV3Qo0YTHQ9sIBFx7e.com",
            "http://08NTMJJYkFi0vl8UUvFvMAwsOpnjPNMGXtQp.com",
            "http://rLtxkeIIvYJLlftABw8CkaxjZaPOFI6Bp2Rg.com",
            "http://vKaEqhYiILe8CGcEgwsK7ARTqAHx5j1ZEp5v.com",
            "http://arv5reKLWFsQMyHG4qsX3y3xwi04DY3ytwOS.com",
            "http://SvdUsoaRgAKoNDmV1esRz3TPjF76bZ5DCLlu.com",
            "http://7bZi8UKjA6mXOGLaMwLuuw2VWAh4BFaqR16x.com",
            "http://8e1d1dtBeDYl9aXjWATnsCgb18hfyllGN5GV.com",
            "http://d7OcQ7kVK1iob02jMaku3ZZbPEpNidGJwM6r.com",
            "http://7TWsfZeqrwoOSxEn4j1h8IEEwTrpexI06kDe.com",
            "http://CgzWl1s001vXzQuCuvSYKUKDXFXxhQi7qxi2.com",
            "http://zq5F1OB7aY0ASw0hf5C5RkUijzlDiCAaIuFh.com",
            "http://keT8eivs7uxHtkrPrp0XgnduihSnZsi9LxdM.com",
            "http://sqR8n3Nf4FC0ERxNkTbvnlKBHeZqYEUwNqtH.com",
            "http://6ysSYQGtGcP5cD1PK2qemhTROrUNkUpMeRqb.com",
            "http://Fc56he9GNzdfkGhJuVL0H431BpsejMm35Ttr.com",
            "http://SCrBXydp3BR6PeIujMIL6fMhhaXF11FlvPwi.com",
            "http://0dEcE2aisA0Z7sWHmvEOvunCu5isVz79IjWH.com",
            "http://2wTFSsei39Dr6UjHhquK1S56hs9GIOUexWym.com",
            "http://VhKpMdSfMH28V6zukprvxRgsBD4F4gKrOe5q.com",
            "http://CzozkCHpma7XJ0yCf1x7Rd4lNcQeVKZOjQK9.com"
    )

    private val sha1Hashes = listOf(
            "3F44E93227041FD770A0A19019E26D72121B0C7D",
            "B704FD9BAEB2E3DEC5016FD2AE5CD41B989EAC5A",
            "7D430E48339256C7121CDAC7BB2B6FC4EA4D6EC3",
            "3E1DE23770C795F9D203E87C7C07855D1739ADD5",
            "B43A2401ECD94E4A4FBD0A1FEAA0171E9F4F3515",
            "57CF7D6914A1D0CB9AA5F6D6A65B1EDC8D23CE0C",
            "FDC40F39C5CF4786E0E4E8D8257A379C4A442484",
            "6687A546C532BF0DF3F3755A028406154A04CFCD",
            "7B5CE9835F34106A01B753189C6B1BCAD7CFF4F3",
            "C6B45A11394543391F6BB63C3E3A11518F7D7D06",
            "9B31207295C004BDA05FA5E8C538CC5FCF7CC30F",
            "D0B27767FAE28F4DC101B735D5DF6161FDCED3E3",
            "678491DC04158A697256128D0F2548B261A590BF",
            "7B2B5366C13619A31AA74DE420E4122BE3E4F441",
            "0B76D625E3006FF0BAA7B6B81D554459DE75ED3A",
            "E0F31A62ABD5067CA6437C84649B7636F884C7FC",
            "7CACF5574D62D55DA20B26F95A0711366FC2A368",
            "AFE3AB594C9E8233E34DC2AE4A0DFA8102CC879F",
            "887AA704F3A76463C7490F23052113718D538DB0",
            "C05F04ECC0393F34ADD89DEC4EFFAFF32322A092",
            "C7CDCA1FBCC99B6D3406839E552782DBA8FB6F6D",
            "C2B644525244499E0775087F0A82F8C0019FDC00",
            "624ECE1290099CBBDD211B8C5F4FEF24B176690F",
            "159F4F14EE3D08B297B9C565C54759A3D5F8F1BF",
            "B8CFEEBD7B7A958D26548C1542A5E0E1F51560C3",
            "70C63A4A0C3E79B7FFE65C9BF004E92B10AE66E4",
            "F8775A62AB5C1C91ED92B88DEA4B3FDCF026437A",
            "EFBCA86A75E458F168F098461749C150E854D98D",
            "D7E1D233FEF01699F77C5AC173DFFC72E9B5F725",
            "633921825B411DCC49D713F0272D1C8D0CC8084E"
    )

    private val md5Hashes = listOf(
            "E11CF56B88EFA89751DA6EFBC0493A50",
            "AA2F2B137C6FC8AE2C7D5831E3C4CC2F",
            "3F9E1A94ADA51AABA53D6F0A4310F0D7",
            "3D06103E4E3E03B9DDBC766198DAA310",
            "4B3551796A333A25365914AA88311778",
            "DE22B936A334612C0D2E66213A913FEE",
            "80231104CFC825E26B0A9D06E6B57066",
            "C7E6D72579FA7201D98AF2CEE7967C3F",
            "4FDF05DF523ED88656D733E9E6682983",
            "7528D9F908964877E0E5DB20D115DBBC",
            "22EE70DA69063ABB59FAD0C534C63ACD",
            "DAF956E67967C4E497FA772B6A0C7D9F",
            "41ADB4F98234071336B7C5BC594480EC",
            "261562352CFD6F7D7B834BDB131F1792",
            "466084BE7EF16339C57629E4BA853659",
            "BB18A5AB6DB78A9E026D38A465F06AC0",
            "DB62A8991DB94F2931AD8DB3106B4AC1",
            "4C6A6ECCC3284515FF6BAC485DBE9805",
            "E3DF5BD09DDD8FF8DF00F61F1E5C4138",
            "291A246CEB867EDC42C0414D9EB19F2B",
            "0CD3A3C4338BA7CA608A39A5EA1B56F4",
            "1FD8EBA8AC271178165CE808031DFF21",
            "5E84F5C8FF52876DC2A3597641B872EA",
            "DB7DE355593CCBDCF00B32CC51170D2E",
            "A1DDE6B52E67B6AA61CAC0662016BBB1",
            "3825E5C1C104C5FAAD933DC1A35336CE",
            "22853087F665ED65767BBA4D7EE94DDF",
            "28C842BAA4588B0B43411EAE3A5A2BCE",
            "9FF42556F8DB61250FCAAE5CBCCCDBD7",
            "1BB0FA0367B4D1B085E2251986491757"
    )

    private val defaultHashes = Arrays.asList(
            "-5281157219898846028",
            "-5545228037119756562",
            "-4445508738195973520",
            "-8775778929669287221",
            "2211503281753659381",
            "2574643105288079458",
            "6701924281059012114",
            "-1458416431602682925",
            "-4472804809269123843",
            "4185999049885461741",
            "7931505461983434721",
            "-8547507448543882352",
            "1917672731789240071",
            "2620616050395488867",
            "-3176894309051000537",
            "-8020058083733704941",
            "-2619339451889455300",
            "7262884140052821962",
            "8800054447309589004",
            "3795015590844041611",
            "668905180262575267",
            "6469343949939806665",
            "-5436097962276741517",
            "7898874369278310528",
            "4197366868288841847",
            "-1545880775175904332",
            "-3483490521335945332",
            "8727677260113125315",
            "-7907885646038731179",
            "-5622045339734506459"
    )

    @Test
    @Throws(Exception::class)
    fun testHash() {
        val sha1Hasher = Hasher(mock(), MessageDigest.getInstance("SHA-1")) { Uri.parse(it) }
        val md5Hasher = Hasher(mock(), MessageDigest.getInstance("MD5")) { Uri.parse(it) }
        val defaultHasher = Hasher(mock(), null) { Uri.parse(it) }

        for (i in strings.indices) {
            assertEqualsWithContext(
                    sha1Hashes[i],
                    sha1Hasher.hash(getRequestMetadata(strings[i], i, "SHA-1 hashing failed"))!!.urlHash,
                    "SHA-1 hash failed at position $i"
            )

            assertEqualsWithContext(
                    md5Hashes[i],
                    md5Hasher.hash(getRequestMetadata(strings[i], i, "MD5 hashing failed"))!!.urlHash,
                    "MD5 hash failed at position $i"
            )

            assertEqualsWithContext(
                    defaultHashes[i],
                    defaultHasher.hash(getRequestMetadata(strings[i], i, "Default hashing failed"))!!.urlHash,
                    "Default hash failed at position $i"
            )
        }
    }

    private fun getRequestMetadata(url: String,
                                   index: Int,
                                   context: String): RequestMetadata.UnHashed {
        val params = getParams(url, index)
        val urlAndParams = if (params == null) url else "$url?$params"

        checkParamsInOrder(urlAndParams, index, context)

        return RequestMetadata.UnHashed(
                String::class.java,
                urlAndParams,
                null//TODO body
        )
    }

    private fun getParams(url: String,
                          index: Int): String? {
        return if (index % 2 == 0) null
        else {
            val numParams = paramsKeys.length
            val length = Math.floor(url.length / numParams.toDouble()).toInt()

            StringBuilder().apply {
                for (i in 0 until numParams) {
                    val startIndex = i * numParams
                    val substring = url.substring(startIndex, startIndex + length)
                    if (!isEmpty()) append("&")
                    append("${paramsKeys[i]}=$substring")
                }
            }.toString()
        }
    }

    private fun checkParamsInOrder(url: String,
                                   index: Int,
                                   context: String) {
        if (index % 2 == 1) {
            val params = target.getSortedParameters(Uri.parse(url))
            val keys = StringBuilder().apply {
                Uri.parse("http://test.com?$params").queryParameterNames.forEach {
                    append(it)
                }
            }.toString()

            assertEqualsWithContext(
                    paramsKeysInOrder,
                    keys,
                    "Parameters were not in order at index $index",
                    context
            )
        }
    }

}
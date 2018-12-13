package uk.co.glass_software.android.cache_interceptor.configuration

import junit.framework.TestCase.assertEquals
import org.junit.Test
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse

class CacheInstructionSerialiserUnitTest {

    private fun newCacheInstruction(operation: CacheInstruction.Operation) =
            CacheInstruction(TestResponse::class.java, operation)

    private val map = LinkedHashMap<String, CacheInstruction>().apply {
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:DO_NOT_CACHE:", newCacheInstruction(DoNotCache))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:INVALIDATE:", newCacheInstruction(Invalidate))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CLEAR:uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:true", newCacheInstruction(Clear(TestResponse::class.java, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CLEAR:uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:false", newCacheInstruction(Clear(TestResponse::class.java)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CLEAR:null:false", newCacheInstruction(Clear()))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:true:true:true:true:true", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:true:true:true:true:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:true:true:true:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:true:true:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:true:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:4321:false:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:1234:null:false:null:null:null:false", newCacheInstruction(Cache(1234L)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:CACHE:null:null:false:null:null:null:false", newCacheInstruction(Cache()))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:REFRESH:1234:4321:true:true:null:null:true", newCacheInstruction(Refresh(1234L, 4321L, true, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:REFRESH:1234:4321:true:true:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true, true)))
        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:REFRESH:1234:4321:true:null:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true)))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:INVALIDATE:", newCacheInstruction(Refresh(1234L, 4321L)))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:INVALIDATE:", newCacheInstruction(Refresh(1234L)))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:INVALIDATE:", newCacheInstruction(Refresh()))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:OFFLINE:null:0:false:null:null:null:false", newCacheInstruction(Offline(true, true)))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:OFFLINE:null:0:false:null:null:null:false", newCacheInstruction(Offline(true)))
//        put("uk.co.glass_software.android.cache_interceptor.test.network.model.TestResponse:OFFLINE:null:0:false:null:null:null:false", newCacheInstruction(Offline()))
    }

    @Test
    fun serialise() {
        map.entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value.operation.type} at position $index could not be serialised",
                    entry.key,
                    entry.value.toString()
            )
        }
    }

    @Test
    fun deserialise() {
        map.entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value.operation.type} at position $index could not be deserialised",
                    CacheInstructionSerialiser.deserialise(entry.key)!!.toString(),
                    entry.value.toString()
            )
        }
    }
}
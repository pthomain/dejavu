package uk.co.glass_software.android.dejavu.configuration

import junit.framework.TestCase.assertEquals
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse

class CacheInstructionSerialiserUnitTest {


    private fun getMap(targetClass: Class<*>) = targetClass.name.let {

        fun newCacheInstruction(operation: CacheInstruction.Operation) =
                CacheInstruction(targetClass, operation)

        LinkedHashMap<String, CacheInstruction>().apply {
            put("$it:DO_NOT_CACHE:", newCacheInstruction(DoNotCache))
            put("$it:INVALIDATE:", newCacheInstruction(Invalidate))
            put("$it:CLEAR:$it:true", newCacheInstruction(Clear(targetClass, true)))
            put("$it:CLEAR:$it:false", newCacheInstruction(Clear(targetClass)))
            put("$it:CLEAR:null:false", newCacheInstruction(Clear()))
            put("$it:CACHE:1234:4321:true:true:true:true:true", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:true:true:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:true:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true)))
            put("$it:CACHE:1234:4321:true:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true)))
            put("$it:CACHE:1234:4321:false:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L)))
            put("$it:CACHE:1234:null:false:null:null:null:false", newCacheInstruction(Cache(1234L)))
            put("$it:CACHE:null:null:false:null:null:null:false", newCacheInstruction(Cache()))
            put("$it:REFRESH:1234:4321:true:true:null:null:true", newCacheInstruction(Refresh(1234L, 4321L, true, true, true)))
            put("$it:REFRESH:1234:4321:true:true:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true, true)))
            put("$it:REFRESH:1234:4321:true:null:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true)))
            put("$it:REFRESH:1234:4321:false:null:null:null:false", newCacheInstruction(Refresh(1234L, 4321L)))
            put("$it:REFRESH:1234:null:false:null:null:null:false", newCacheInstruction(Refresh(1234L)))
            put("$it:REFRESH:null:null:false:null:null:null:false", newCacheInstruction(Refresh()))
            put("$it:OFFLINE:null:null:true:true:null:null:false", newCacheInstruction(Offline(true, true)))
            put("$it:OFFLINE:null:null:true:null:null:null:false", newCacheInstruction(Offline(true)))
            put("$it:OFFLINE:null:null:false:null:null:null:false", newCacheInstruction(Offline()))
        }
    }

    @Test
    fun serialiseTopLevelClass() {
        serialise(TestResponse::class.java)
    }

    @Test
    fun deserialiseTopLevelClass() {
        deserialise(TestResponse::class.java)
    }

    @Test
    fun serialiseInnerClass() {
        serialise(Parent.InnerClass::class.java)
    }

    @Test
    fun deserialiseInnerClass() {
        deserialise(Parent.InnerClass::class.java)
    }

    interface Parent {
        class InnerClass
    }

    private fun serialise(targetClass: Class<*>) {
        getMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value.operation.type} at position $index could not be serialised",
                    entry.key,
                    entry.value.toString()
            )
        }
    }

    private fun deserialise(targetClass: Class<*>) {
        getMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value.operation.type} at position $index could not be deserialised",
                    CacheInstructionSerialiser.deserialise(entry.key),
                    entry.value
            )
        }
    }


}
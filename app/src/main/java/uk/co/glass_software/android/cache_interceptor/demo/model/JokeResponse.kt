package uk.co.glass_software.android.cache_interceptor.demo.model

class JokeResponse {

    var type: String? = null
    var value: Value? = null

    inner class Value {
        var joke: String? = null
    }
}
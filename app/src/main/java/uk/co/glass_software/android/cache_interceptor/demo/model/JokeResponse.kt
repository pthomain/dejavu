package uk.co.glass_software.android.cache_interceptor.demo.model

import com.google.gson.annotations.SerializedName

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder

import uk.co.glass_software.android.cache_interceptor.demo.R
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.MetadataHolder

class JokeResponse{

    @SerializedName("type")
    var type: String? = null

    @SerializedName("value")
    var value: Value? = null

    override fun equals(o: Any?): Boolean {

        if (this === o) {
            return true
        }

        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val joke = o as JokeResponse?

        return EqualsBuilder()
                .append(type, joke!!.type)
                .append(value, joke.value)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37)
                .append(type)
                .append(value)
                .toHashCode()
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("type", type)
                .append("value", value)
                .toString()
    }
}
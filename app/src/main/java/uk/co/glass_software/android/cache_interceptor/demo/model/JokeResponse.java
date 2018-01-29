package uk.co.glass_software.android.cache_interceptor.demo.model;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.response.EmptyResponse;

public class JokeResponse extends EmptyResponse {
    
    @Override
    public float getTtlInMinutes() {
        return .5f; //30s
    }
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("value")
    private Value value;
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Value getValue() {
        return value;
    }
    
    public void setValue(Value value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        JokeResponse joke = (JokeResponse) o;
        
        return new EqualsBuilder()
                .append(type, joke.type)
                .append(value, joke.value)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(value)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("value", value)
                .toString();
    }
}
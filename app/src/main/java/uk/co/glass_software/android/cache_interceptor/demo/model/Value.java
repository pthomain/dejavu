package uk.co.glass_software.android.cache_interceptor.demo.model;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Value {
    
    @SerializedName("joke")
    private String joke;
    
    public String getJoke() {
        return joke;
    }
    
    public void setJoke(String joke) {
        this.joke = joke;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        Value value = (Value) o;
        
        return new EqualsBuilder()
                .append(joke, value.joke)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(joke)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("joke", joke)
                .toString();
    }
}
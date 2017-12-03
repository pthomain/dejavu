package uk.co.glass_software.android.cache_interceptor.demo.model;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class Value {
    
    @SerializedName("id")
    private Integer id;
    
    @SerializedName("joke")
    private String joke;
    
    @SerializedName("categories")
    private List<Object> categories = null;
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getJoke() {
        return joke;
    }
    
    public void setJoke(String joke) {
        this.joke = joke;
    }
    
    public List<Object> getCategories() {
        return categories;
    }
    
    public void setCategories(List<Object> categories) {
        this.categories = categories;
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
                .append(id, value.id)
                .append(joke, value.joke)
                .append(categories, value.categories)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(joke)
                .append(categories)
                .toHashCode();
    }
    
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("joke", joke)
                .append("categories", categories)
                .toString();
    }
}
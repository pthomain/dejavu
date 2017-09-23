
package uk.co.glass_software.android.cache_interceptor.base.network.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Geo {

    @SerializedName("lat")
    @Expose
    private String lat;
    @SerializedName("lng")
    @Expose
    private String lng;

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        Geo geo = (Geo) o;
        
        return new EqualsBuilder()
                .append(lat, geo.lat)
                .append(lng, geo.lng)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(lat)
                .append(lng)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("lat", lat)
                .append("lng", lng)
                .toString();
    }
}

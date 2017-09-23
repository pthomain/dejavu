
package uk.co.glass_software.android.cache_interceptor.base.network.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Address {

    @SerializedName("street")
    @Expose
    private String street;
    @SerializedName("suite")
    @Expose
    private String suite;
    @SerializedName("city")
    @Expose
    private String city;
    @SerializedName("zipcode")
    @Expose
    private String zipcode;
    @SerializedName("geo")
    @Expose
    private Geo geo;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        Address address = (Address) o;
        
        return new EqualsBuilder()
                .append(street, address.street)
                .append(suite, address.suite)
                .append(city, address.city)
                .append(zipcode, address.zipcode)
                .append(geo, address.geo)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(street)
                .append(suite)
                .append(city)
                .append(zipcode)
                .append(geo)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("street", street)
                .append("suite", suite)
                .append("city", city)
                .append("zipcode", zipcode)
                .append("geo", geo)
                .toString();
    }
}

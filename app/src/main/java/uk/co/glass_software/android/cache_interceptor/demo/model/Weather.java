package uk.co.glass_software.android.cache_interceptor.demo.model;

public class Weather {
    
    private String title;
    private String locationType;
    private Integer woeid;
    private String lattLong;
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getLocationType() {
        return locationType;
    }
    
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
    
    public Integer getWoeid() {
        return woeid;
    }
    
    public void setWoeid(Integer woeid) {
        this.woeid = woeid;
    }
    
    public String getLattLong() {
        return lattLong;
    }
    
    public void setLattLong(String lattLong) {
        this.lattLong = lattLong;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        Weather weather = (Weather) o;
        
        if (title != null ? !title.equals(weather.title) : weather.title != null) {
            return false;
        }
        if (locationType != null ? !locationType.equals(weather.locationType) : weather.locationType != null) {
            return false;
        }
        if (woeid != null ? !woeid.equals(weather.woeid) : weather.woeid != null) {
            return false;
        }
        return lattLong != null ? lattLong.equals(weather.lattLong) : weather.lattLong == null;
    }
    
    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (locationType != null ? locationType.hashCode() : 0);
        result = 31 * result + (woeid != null ? woeid.hashCode() : 0);
        result = 31 * result + (lattLong != null ? lattLong.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Weather{");
        sb.append("title='").append(title).append('\'');
        sb.append(", locationType='").append(locationType).append('\'');
        sb.append(", woeid=").append(woeid);
        sb.append(", lattLong='").append(lattLong).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

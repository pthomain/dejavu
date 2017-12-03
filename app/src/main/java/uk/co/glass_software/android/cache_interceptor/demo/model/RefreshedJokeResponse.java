package uk.co.glass_software.android.cache_interceptor.demo.model;

public class RefreshedJokeResponse extends JokeResponse {
    
    @Override
    public boolean isRefresh() {
        return true;
    }
}

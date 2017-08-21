package uk.co.glass_software.android.cache_interceptor.demo;

import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

public interface DemoPresenter {
    
    void loadResponse(String location,
                      Callback<String> onNext,
                      Action onEnd);
    
}

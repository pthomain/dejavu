package uk.co.glass_software.android.cache_interceptor.base;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

import uk.co.glass_software.android.cache_interceptor.BuildConfig;

public class CustomRobolectricTestRunner extends RobolectricTestRunner {
    
    public CustomRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        System.setProperty("android.package", BuildConfig.APPLICATION_ID);
        String folder = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE;
        System.setProperty("android.manifest",
                           "build/intermediates/manifests/aapt/" + folder + "/AndroidManifest.xml"
        );
        System.setProperty("android.resources",
                           "build/intermediates/res/merged/" + folder
        );
        System.setProperty("android.assets", "build/intermediates/assets/" + folder);
    }
    
}

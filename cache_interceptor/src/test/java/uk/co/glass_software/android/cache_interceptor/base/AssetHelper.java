package uk.co.glass_software.android.cache_interceptor.base;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class AssetHelper {
    
    private final Gson gson;
    private final Logger logger;
    private final String assetsFolder;
    
    public AssetHelper(String assetsFolder,
                       Gson gson,
                       Logger logger) {
        this.assetsFolder = assetsFolder;
        this.gson = gson;
        this.logger = logger;
    }
    
    public <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> Observable<R> getStubbedResponse(String fileName,
                                                                                                                                  Class<R> responseClass) {
        return observeFile(fileName).map(json -> gson.fromJson(json, responseClass));
    }
    
    Observable<String> observeFile(String fileName) {
        InputStream inputStream = null;
        
        try {
            try {
                File file = new File(assetsFolder + fileName);
                inputStream = new FileInputStream(file);
                return Observable.just(fileToString(inputStream));
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        catch (IOException e) {
            String message = "An error occurred while trying to read "
                             + "file: "
                             + fileName;
            logger.e(getClass().getSimpleName(),
                     e,
                     message
            );
            return Observable.error(new ApiError(
                    e,
                    ApiError.NON_HTTP_STATUS,
                    ErrorCode.UNKNOWN,
                    message
            ));
        }
    }
    
    static String fileToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            return builder.toString();
        }
        finally {
            reader.close();
        }
    }
    
}

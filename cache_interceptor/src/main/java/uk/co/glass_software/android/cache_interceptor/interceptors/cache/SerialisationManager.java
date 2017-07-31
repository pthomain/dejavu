package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.iq80.snappy.Snappy;

import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

class SerialisationManager {
    
    private final Logger logger;
    private final Gson gson;
    
    SerialisationManager(Logger logger,
                         Gson gson) {
        this.logger = logger;
        this.gson = gson;
    }
    
    @Nullable
    <E, R extends CacheToken.Holder<R, E>> R uncompress(Class<R> responseClass,
                                                        byte[] compressedData,
                                                        Action onError) {
        String simpleName = responseClass.getSimpleName();
        
        try {
            byte[] uncompressed = Snappy.uncompress(compressedData, 0, compressedData.length);
            logCompression(compressedData, simpleName, uncompressed);
            return gson.fromJson(new String(uncompressed), responseClass);
        }
        catch (JsonSyntaxException e) {
            logger.e(this, "Cached data didn't match "
                           + simpleName
                           + ": flushing cache"
            );
            onError.act();
            return null;
        }
        catch (Exception e) {
            onError.act();
            logger.e(this, "Could not uncompress " + simpleName);
            return null;
        }
    }
    
    <E, R extends CacheToken.Holder<R, E>> byte[] compress(R response) {
        String json = gson.toJson(response);
        String simpleName = response.getClass().getSimpleName();
        
        try {
            byte[] compressed = Snappy.compress(json.getBytes());
            logCompression(compressed, simpleName, json.getBytes());
            return compressed;
        }
        catch (Exception e) {
            logger.e(this, "Could not compress " + simpleName);
            return json.getBytes();
        }
    }
    
    private void logCompression(byte[] compressedData,
                                String simpleName,
                                byte[] uncompressed) {
        logger.d(this,
                 "Compressed/uncompressed "
                 + simpleName
                 + ": "
                 + compressedData.length
                 + "B/"
                 + uncompressed.length
                 + "B ("
                 + (100 * compressedData.length / uncompressed.length)
                 + "%)"
        );
    }
}

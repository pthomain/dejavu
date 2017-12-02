package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.iq80.snappy.Snappy;

import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory;

class SerialisationManager {
    
    private final Logger logger;
    private final StoreEntryFactory storeEntryFactory;
    private final boolean encryptData;
    private final boolean compressData;
    private final Gson gson;
    
    SerialisationManager(Logger logger,
                         StoreEntryFactory storeEntryFactory,
                         boolean encryptData,
                         boolean compressData,
                         Gson gson) {
        this.logger = logger;
        this.storeEntryFactory = storeEntryFactory;
        this.encryptData = encryptData;
        this.compressData = compressData;
        this.gson = gson;
    }
    
    @Nullable
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R deserialise(Class<R> responseClass,
                                                                                                        byte[] data,
                                                                                                        Action onError) {
        String simpleName = responseClass.getSimpleName();
        
        try {
            byte[] uncompressed;
            
            if (compressData) {
                uncompressed = Snappy.uncompress(data, 0, data.length);
                logCompression(data, simpleName, uncompressed);
            }
            else {
                uncompressed = data;
            }
            
            if (encryptData) {
                uncompressed = storeEntryFactory.decrypt(uncompressed);
            }
            
            return gson.fromJson(new String(uncompressed), responseClass);
        }
        catch (JsonSyntaxException e) {
            logger.e(this,
                     e,
                     "Cached data didn't match "
                     + simpleName
                     + ": flushing cache"
            );
            onError.act();
            return null;
        }
        catch (Exception e) {
            logger.e(this,
                     e,
                     "Could not deserialise "
                     + simpleName
                     + ": flushing cache"
            );
            onError.act();
            return null;
        }
    }
    
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> byte[] serialise(R response) {
        String simpleName = response.getClass().getSimpleName();
        
        byte[] data = gson.toJson(response).getBytes();
        
        if (encryptData) {
            data = storeEntryFactory.encrypt(data);
        }
        
        if (compressData) {
            byte[] compressed = Snappy.compress(data);
            logCompression(compressed, simpleName, data);
            data = compressed;
        }
        
        return data;
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

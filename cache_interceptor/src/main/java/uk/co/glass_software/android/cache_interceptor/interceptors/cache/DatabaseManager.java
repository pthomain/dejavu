package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import uk.co.glass_software.android.utils.Function;
import uk.co.glass_software.android.utils.Logger;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

class DatabaseManager {
    
    private final Logger logger;
    private final SQLiteDatabase db;
    private final SerialisationManager serialisationManager;
    private final DateFormat dateFormat;
    private final Function<Long, Date> dateFactory;
    private final Function<Map<String, ?>, ContentValues> contentValuesFactory;
    
    DatabaseManager(SQLiteDatabase db,
                    SerialisationManager serialisationManager,
                    Logger logger,
                    Function<Long, Date> dateFactory,
                    Function<Map<String, ?>, ContentValues> contentValuesFactory) {
        this.db = db;
        this.serialisationManager = serialisationManager;
        this.logger = logger;
        this.dateFactory = dateFactory;
        this.contentValuesFactory = contentValuesFactory;
        dateFormat = new SimpleDateFormat("MMM dd h:m:s", Locale.UK);
    }
    
    void flushCache() {
        db.execSQL("DELETE FROM " + SqlOpenHelper.TABLE_CACHE);
        logger.d(this, "Cleared entire HTTP cache");
    }
    
    @Nullable
    @SuppressWarnings("unchecked")
    <E, R extends CacheToken.Holder<R, E>> R getCachedResponse(Observable<R> upstream,
                                                               CacheToken cacheToken) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Checking for cached " + simpleName);
        
        String[] projection = {
                SqlOpenHelper.COLUMN_CACHE_DATE,
                SqlOpenHelper.COLUMN_CACHE_EXPIRY_DATE,
                SqlOpenHelper.COLUMN_CACHE_DATA
        };
        
        String selection = SqlOpenHelper.COLUMN_CACHE_TOKEN + " = ?";
        String[] selectionArgs = {cacheToken.getKey()};
        
        Cursor cursor = db.query(SqlOpenHelper.TABLE_CACHE,
                                 projection,
                                 selection,
                                 selectionArgs,
                                 null,
                                 null,
                                 null,
                                 "1"
        );
        
        try {
            if (cursor.getCount() != 0 && cursor.moveToNext()) {
                logger.d(this, "Found a cached " + simpleName);
                
                Date cacheDate = dateFactory.get(cursor.getLong(0));
                Date expiryDate = dateFactory.get(cursor.getLong(1));
                byte[] compressedData = cursor.getBlob(2);
                
                return (R) getCachedResponse(upstream,
                                             cacheToken,
                                             cacheDate,
                                             expiryDate,
                                             compressedData
                );
            }
            else {
                logger.d(this, "Found no cached " + simpleName);
                return null;
            }
        }
        finally {
            cursor.close();
        }
    }
    
    @Nullable
    @SuppressWarnings("unchecked")
    private <E, R extends CacheToken.Holder<R, E>> R getCachedResponse(Observable<R> upstream,
                                                            CacheToken<R> cacheToken,
                                                            Date cacheDate,
                                                            Date expiryDate,
                                                            byte[] compressedData) {
        R response = serialisationManager.uncompress(cacheToken.getResponseClass(),
                                                     compressedData,
                                                     this::flushCache
        );
        
        if (response != null) {
            response.setCacheToken(CacheToken.cached(cacheToken,
                                                     upstream,
                                                     cacheDate,
                                                     expiryDate
            ));
        }
        
        logger.d(this, "Returning cached "
                       + cacheToken.getResponseClass().getSimpleName()
                       + " cached until "
                       + dateFormat.format(expiryDate)
        );
        return response;
    }
    
    <E, R extends CacheToken.Holder<R, E>> void cache(R response) {
        CacheToken<?> cacheToken = response.getCacheToken();
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Caching " + simpleName);
        
        byte[] compressed = serialisationManager.compress(response);
        
        if (compressed != null) {
            String hash = cacheToken.getKey();
            
            Map<String, Object> values = new HashMap<>();
            values.put(SqlOpenHelper.COLUMN_CACHE_TOKEN, hash);
            values.put(SqlOpenHelper.COLUMN_CACHE_DATE, cacheToken.getCacheDate().getTime());
            values.put(SqlOpenHelper.COLUMN_CACHE_EXPIRY_DATE, cacheToken.getExpiryDate().getTime());
            values.put(SqlOpenHelper.COLUMN_CACHE_DATA, compressed);
            
            db.insertWithOnConflict(
                    SqlOpenHelper.TABLE_CACHE,
                    null,
                    contentValuesFactory.get(values),
                    CONFLICT_REPLACE
            );
        }
        else {
            logger.e(this, "Could not compress and store data for " + simpleName);
        }
    }
    
}

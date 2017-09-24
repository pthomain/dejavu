package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

class DatabaseManager {
    
    private final static long DEFAULT_CLEANUP_THRESHOLD_IN_MINUTES = 7 * 24 * 60; // 1 week
    private final static SimpleDateFormat CLEANUP_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.UK
    );
    
    private final Logger logger;
    private final SQLiteDatabase db;
    private final SerialisationManager serialisationManager;
    private final DateFormat dateFormat;
    private final long cleanUpThresholdInMillis;
    private final Function<Long, Date> dateFactory;
    private final Function<Map<String, ?>, ContentValues> contentValuesFactory;
    private final Hasher hasher;
    
    DatabaseManager(SQLiteDatabase db,
                    SerialisationManager serialisationManager,
                    Logger logger,
                    Function<Long, Date> dateFactory,
                    Function<Map<String, ?>, ContentValues> contentValuesFactory) {
        this(db,
             serialisationManager,
             logger,
             DEFAULT_CLEANUP_THRESHOLD_IN_MINUTES,
             dateFactory,
             contentValuesFactory
        );
    }
    
    DatabaseManager(SQLiteDatabase db,
                    SerialisationManager serialisationManager,
                    Logger logger,
                    long cleanUpThresholdInMinutes,
                    Function<Long, Date> dateFactory,
                    Function<Map<String, ?>, ContentValues> contentValuesFactory) {
        this.db = db;
        this.serialisationManager = serialisationManager;
        this.logger = logger;
        this.cleanUpThresholdInMillis = cleanUpThresholdInMinutes * 60 * 1000;
        this.dateFactory = dateFactory;
        this.contentValuesFactory = contentValuesFactory;
        dateFormat = new SimpleDateFormat("MMM dd h:m:s", Locale.UK);
        hasher = getHasher();
    }
    
    private Hasher getHasher() {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            logger.d(this, "Using SHA-1 hasher");
        }
        catch (NoSuchAlgorithmException e) {
            logger.e(this, "Could not create a SHA-1 message digest");
            messageDigest = null;
        }
        
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("MD5");
                logger.d(this, "Using MD5 hasher");
            }
            catch (NoSuchAlgorithmException e) {
                logger.e(this, "Could not create a MD5 message digest");
                messageDigest = null;
            }
        }
        
        if (messageDigest == null) {
            return null; //TODO
        }
        else {
            return new Hasher(messageDigest);
        }
    }
    
    void clearOlderEntries() {
        Date date = new Date(System.currentTimeMillis() + cleanUpThresholdInMillis);
        String sqlDate = CLEANUP_DATE_FORMAT.format(date);
        
        int deleted = db.delete(SqlOpenHelper.TABLE_CACHE,
                                SqlOpenHelper.COLUMN_CACHE_EXPIRY_DATE + " < ?",
                                new String[]{"date('" + sqlDate + "')"}
        );
        
        logger.d(this, "Cleared " + deleted + " old entrie(s) from HTTP cache");
    }
    
    void flushCache() {
        db.execSQL("DELETE FROM " + SqlOpenHelper.TABLE_CACHE);
        logger.d(this, "Cleared entire HTTP cache");
    }
    
    @Nullable
    @SuppressWarnings("unchecked")
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R getCachedResponse(Observable<R> upstream,
                                                                                                              CacheToken cacheToken) {
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Checking for cached " + simpleName);
        
        String[] projection = {
                SqlOpenHelper.COLUMN_CACHE_DATE,
                SqlOpenHelper.COLUMN_CACHE_EXPIRY_DATE,
                SqlOpenHelper.COLUMN_CACHE_DATA
        };
        
        String selection = SqlOpenHelper.COLUMN_CACHE_TOKEN + " = ?";
        String[] selectionArgs = {cacheToken.getKey(hasher)};
        
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
    private <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R getCachedResponse(Observable<R> upstream,
                                                                                                                      CacheToken<R> cacheToken,
                                                                                                                      Date cacheDate,
                                                                                                                      Date expiryDate,
                                                                                                                      byte[] compressedData) {
        R response = serialisationManager.uncompress(cacheToken.getResponseClass(),
                                                     compressedData,
                                                     this::flushCache
        );
        
        if (response != null) {
            response.setMetadata(ResponseMetadata.create(
                    CacheToken.cached(cacheToken,
                                      upstream,
                                      cacheDate,
                                      expiryDate
                    ),
                    null
            ));
        }
        
        logger.d(this, "Returning cached "
                       + cacheToken.getResponseClass().getSimpleName()
                       + " cached until "
                       + dateFormat.format(expiryDate)
        );
        return response;
    }
    
    <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> void cache(R response) {
        CacheToken<?> cacheToken = response.getMetadata().getCacheToken();
        String simpleName = cacheToken.getResponseClass().getSimpleName();
        logger.d(this, "Caching " + simpleName);
        
        byte[] compressed = serialisationManager.compress(response);
        
        if (compressed != null) {
            String hash = cacheToken.getKey(hasher);
            
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

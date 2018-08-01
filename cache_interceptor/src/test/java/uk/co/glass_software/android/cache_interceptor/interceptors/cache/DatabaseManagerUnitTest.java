package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Map;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DatabaseManagerUnitTest {
    
    private SQLiteDatabase mockDb;
    private SerialisationManager mockSerialisationManager;
    private Observable<TestResponse> mockObservable;
    private CacheToken mockCacheToken;
    private Cursor mockCursor;
    private TestResponse mockResponse;
    private Function mockContentValuesFactory;
    private ResponseMetadata mockMetadata;
    private String cacheKey;
    private byte[] mockBlob;
    
    private Function mockDateProvider;
    private final Long currentDateTime = 239094L;
    private Date mockCurrentDate;
    private final Long mockFetchDateTime = 182938L;
    private Date mockFetchDate;
    private final long mockCacheDateTime = 1234L;
    private Date mockCacheDate;
    private final long mockExpiryDateTime = 5678L;
    private Date mockExpiryDate;
    
    private DatabaseManager target;
    
    @Before
    public void setUp() throws Exception {
        Logger mockLogger = mock(Logger.class);
        
        mockDb = mock(SQLiteDatabase.class);
        mockObservable = mock(Observable.class);
        mockSerialisationManager = mock(SerialisationManager.class);
        mockDateProvider = mock(Function.class);
        
        mockCurrentDate = mock(Date.class);
        mockFetchDate = mock(Date.class);
        mockCacheDate = mock(Date.class);
        mockExpiryDate = mock(Date.class);
        
        when(mockCurrentDate.getTime()).thenReturn(currentDateTime);
        when(mockFetchDate.getTime()).thenReturn(mockFetchDateTime);
        when(mockCacheDate.getTime()).thenReturn(mockCacheDateTime);
        when(mockExpiryDate.getTime()).thenReturn(mockExpiryDateTime);
        
        when(mockDateProvider.get(eq(null))).thenReturn(mockCurrentDate);
        when(mockDateProvider.get(eq(mockCacheDateTime))).thenReturn(mockCacheDate);
        when(mockDateProvider.get(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate);
        
        mockContentValuesFactory = mock(Function.class);
        
        mockCacheToken = mock(CacheToken.class);
        mockCursor = mock(Cursor.class);
        mockResponse = mock(TestResponse.class);
        cacheKey = "someKey";
        mockBlob = new byte[]{1, 2, 3, 4, 5, 6, 8, 9};
        
        when(mockCacheToken.getResponseClass()).thenReturn(TestResponse.class);
        when(mockCacheToken.getKey(any(Hasher.class))).thenReturn(cacheKey);
        
        when(mockCacheToken.getFetchDate()).thenReturn(mockFetchDate);
        when(mockCacheToken.getCacheDate()).thenReturn(mockCacheDate);
        when(mockCacheToken.getExpiryDate()).thenReturn(mockExpiryDate);
        
        mockMetadata = mock(ResponseMetadata.class);
        when(mockResponse.getMetadata()).thenReturn(mockMetadata);
        when(mockMetadata.getCacheToken()).thenReturn(mockCacheToken);
        
        target = new DatabaseManager(
                mockDb,
                mockSerialisationManager,
                mockLogger,
                mockDateProvider,
                mockContentValuesFactory
        );
    }
    
    @Test
    public void testFlushCache() throws Exception {
        target.clearCache();
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockDb).execSQL(captor.capture());
        
        assertEquals("Cache flush SQL command was wrong", "DELETE FROM " + Companion.getTABLE_CACHE(), captor.getValue());
    }
    
    @Test
    public void testCache() throws Exception {
        when(mockSerialisationManager.serialise(eq(mockResponse))).thenReturn(mockBlob);
        when(mockContentValuesFactory.get(any())).thenReturn(mock(ContentValues.class));
        
        target.cache(mockResponse);
        
        verify(mockDb).insertWithOnConflict(
                eq(Companion.getTABLE_CACHE()),
                eq(null),
                any(),
                eq(CONFLICT_REPLACE)
        );
        
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockContentValuesFactory).get(mapCaptor.capture());
        Map<String, Object> values = mapCaptor.getValue();
        
        assertEquals("Cache key didn't match", cacheKey, values.get(Companion.getCOLUMN_CACHE_TOKEN()));
        assertEquals("Cache date didn't match", mockCacheDateTime, values.get(Companion.getCOLUMN_CACHE_DATE()));
        assertEquals("Expiry date didn't match", mockExpiryDateTime, values.get(Companion.getCOLUMN_CACHE_EXPIRY_DATE()));
        assertEquals("Cached data didn't match", mockBlob, values.get(Companion.getCOLUMN_CACHE_DATA()));
    }
    
    @Test
    public void testGetCachedResponseWithStaleResult() throws Exception {
        testGetCachedResponse(true, true);
    }
    
    @Test
    public void testGetCachedResponseWithFreshResult() throws Exception {
        testGetCachedResponse(true, false);
    }
    
    @Test
    public void testGetCachedResponseNoResult() throws Exception {
        testGetCachedResponse(false, false);
    }
    
    private void testGetCachedResponse(boolean hasResults,
                                       boolean isStale) {
        String mockUrl = "mockUrl";
        
        doReturn(mockCursor).when(mockDb)
                            .query(eq(Companion.getTABLE_CACHE()),
                                   any(),
                                   any(),
                                   any(),
                                   eq(null),
                                   eq(null),
                                   eq(null),
                                   eq("1")
                            );
        
        if (hasResults) {
            when(mockCursor.getCount()).thenReturn(1);
            when(mockCursor.moveToNext()).thenReturn(true);
            
            when(mockCursor.getLong(eq(0))).thenReturn(mockCacheDateTime);
            when(mockCursor.getLong(eq(1))).thenReturn(mockExpiryDateTime);
            when(mockCursor.getBlob(eq(2))).thenReturn(mockBlob);
            when(mockExpiryDate.getTime()).thenReturn(mockExpiryDateTime);
            
            doReturn(mockResponse).when(mockSerialisationManager)
                                  .deserialise(eq(TestResponse.class),
                                               eq(mockBlob),
                                               any()
                                  );
            
            when(mockCurrentDate.getTime()).thenReturn(mockExpiryDateTime + (isStale ? 1 : -1));
            
            when(mockCacheToken.getApiUrl()).thenReturn(mockUrl);
            when(mockCacheToken.getResponseClass()).thenReturn(TestResponse.class);
        }
        else {
            when(mockCursor.getCount()).thenReturn(0);
        }
        
        TestResponse cachedResponse = target.getCachedResponse(mockObservable, mockCacheToken);
        
        ArgumentCaptor<String[]> projectionCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> selectionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> selectionArgsCaptor = ArgumentCaptor.forClass(String[].class);
        
        verify(mockDb).query(eq(Companion.getTABLE_CACHE()),
                             projectionCaptor.capture(),
                             selectionCaptor.capture(),
                             selectionArgsCaptor.capture(),
                             isNull(),
                             isNull(),
                             isNull(),
                             eq("1")
        );
        
        String[] projection = projectionCaptor.getValue();
        String selection = selectionCaptor.getValue();
        String[] selectionArgs = selectionArgsCaptor.getValue();
        
        assertEquals("Wrong selection", "token = ?", selection);
        
        assertEquals("Wrong projection size", 3, projection.length);
        assertEquals("Wrong projection at position 0", "cache_date", projection[0]);
        assertEquals("Wrong projection at position 1", "expiry_date", projection[1]);
        assertEquals("Wrong projection at position 2", "data", projection[2]);
        
        assertEquals("Wrong selection args size", 1, selectionArgs.length);
        assertEquals("Wrong selection arg at position 0", cacheKey, selectionArgs[0]);
        
        if (hasResults) {
            assertEquals("Cached response didn't match", mockResponse, cachedResponse);
            
            ArgumentCaptor<ResponseMetadata> metadataCaptor = ArgumentCaptor.forClass(ResponseMetadata.class);
            verify(cachedResponse).setMetadata(metadataCaptor.capture());
            CacheToken cacheToken = metadataCaptor.getValue().getCacheToken();
            
            assertEquals("Cached response class didn't match", TestResponse.class, cacheToken.getResponseClass());
            assertEquals("Cache date didn't match", mockCacheDate, cacheToken.getCacheDate());
            assertEquals("Expiry date didn't match", mockExpiryDate, cacheToken.getExpiryDate());
            assertEquals("Refresh observable didn't match", mockObservable, cacheToken.getRefreshObservable());
            assertEquals("Response class didn't match", TestResponse.class, cacheToken.getResponseClass());
            assertEquals("Url didn't match", mockUrl, cacheToken.getApiUrl());
            assertEquals("Cached response should be CACHED", CacheStatus.CACHED, cacheToken.getStatus());
        }
        else {
            assertNull("Cached response should be null", cachedResponse);
        }
        
        verify(mockCursor).close();
    }
}
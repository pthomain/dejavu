package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.CACHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.STALE;

@SuppressWarnings("unchecked")
public class CacheManagerUnitTest {
    
    private Date mockDate;
    private CacheToken mockToken;
    private Function mockDateFactory;
    
    private CacheManager target;
    
    @Before
    public void setUp() throws Exception {
        mockDate = mock(Date.class);
        mockToken = mock(CacheToken.class);
        
        mockDateFactory = mock(Function.class);
        target = new CacheManager(
                mock(DatabaseManager.class),
                mockDateFactory,
                new Gson(),
                mock(Logger.class)
        );
    }
    
    @Test
    public void testGetCachedStatus() throws Exception {
        when(mockToken.getExpiryDate()).thenReturn(null);
        when(mockToken.getStatus()).thenReturn(CACHED);
        
        assertEquals(STALE, target.getCachedStatus(mockToken));
        
        resetMocks();
        
        when(mockDate.getTime()).thenReturn(0L);
        when(mockToken.getExpiryDate()).thenReturn(new Date(1L));
        assertEquals(CACHED, target.getCachedStatus(mockToken));
        
        resetMocks();
        
        when(mockDate.getTime()).thenReturn(1L);
        when(mockToken.getExpiryDate()).thenReturn(new Date(0L));
        assertEquals(STALE, target.getCachedStatus(mockToken));
    }
    
    private void resetMocks() {
        reset(mockToken, mockDate, mockDateFactory);
        when(mockDateFactory.get(isNull())).thenReturn(mockDate);
    }
}
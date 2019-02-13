package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Before
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import java.util.*

class DatabaseManagerUnitTest {

    private lateinit var mockDatabase: SupportSQLiteDatabase
    private lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockContentValuesFactory: (Map<String, *>) -> ContentValues

    private lateinit var mockCompressData: Boolean
    private lateinit var mockEncryptData: Boolean
    private lateinit var mockDurationInMillis: Long

    private lateinit var target: DatabaseManager<Glitch>

    @Before
    fun setUp() {

        target = DatabaseManager(

        )
    }
}
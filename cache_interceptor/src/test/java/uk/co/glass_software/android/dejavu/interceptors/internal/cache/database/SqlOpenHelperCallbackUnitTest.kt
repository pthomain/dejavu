package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.mockito.InOrder

class SqlOpenHelperCallbackUnitTest {

    private lateinit var mockSupportSQLiteDatabase: SupportSQLiteDatabase

    private lateinit var target: SqlOpenHelperCallback

    @Before
    fun setUp() {
        mockSupportSQLiteDatabase = mock()

        target = SqlOpenHelperCallback(1234)
    }

    @Test
    fun testOnCreate() {
        target.onCreate(mockSupportSQLiteDatabase)

        verifyOnCreate()
    }

    private fun verifyOnCreate(inOrder: InOrder = inOrder(mockSupportSQLiteDatabase)) {
        inOrder.apply {
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE TABLE IF NOT EXISTS rx_cache (token TEXT UNIQUE, cache_date INTEGER, expiry_date INTEGER, data NONE, class TEXT, is_encrypted INTEGER, is_compressed INTEGER)")
            )
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE INDEX IF NOT EXISTS token_index ON rx_cache(token)")
            )
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE INDEX IF NOT EXISTS expiry_date_index ON rx_cache(expiry_date)")
            )
        }
    }

    @Test
    fun testOnUpgrade() {
        target.onUpgrade(
                mockSupportSQLiteDatabase,
                1234,
                1235
        )

        verifyOnUpgradeDowngrade()
    }

    private fun verifyOnUpgradeDowngrade() {
        val inOrder = inOrder(mockSupportSQLiteDatabase)
        inOrder.verify(mockSupportSQLiteDatabase).execSQL("DROP TABLE IF EXISTS ${SqlOpenHelperCallback.TABLE_CACHE}")

        verifyOnCreate(inOrder)
    }

    @Test
    fun testOnDowngrade() {
        target.onDowngrade(
                mockSupportSQLiteDatabase,
                1234,
                1235
        )

        verifyOnUpgradeDowngrade()
    }

}
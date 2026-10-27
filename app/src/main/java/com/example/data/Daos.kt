package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surahNumber AND verseNumber = :verseNumber")
    suspend fun deleteBookmark(surahNumber: Int, verseNumber: Int)

    @Query("SELECT EXISTS(SELECT * FROM bookmarks WHERE surahNumber = :surahNumber AND verseNumber = :verseNumber)")
    fun isBookmarked(surahNumber: Int, verseNumber: Int): Flow<Boolean>
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY publishDate DESC")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Query("DELETE FROM news")
    suspend fun clearAllNews()
}

@Dao
interface ScannedProductDao {
    @Query("SELECT * FROM scanned_products ORDER BY timestamp DESC")
    fun getAllScannedProducts(): Flow<List<ScannedProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ScannedProductEntity)

    @Query("DELETE FROM scanned_products WHERE barcode = :barcode")
    suspend fun deleteProduct(barcode: String)

    @Query("DELETE FROM scanned_products")
    suspend fun clearHistory()

    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ScannedProductEntity?
}

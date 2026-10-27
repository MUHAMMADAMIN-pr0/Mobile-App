package com.example.data

import kotlinx.coroutines.flow.Flow

class DeenRepository(private val db: AppDatabase) {
    val bookmarkDao = db.bookmarkDao()
    val newsDao = db.newsDao()
    val scannedProductDao = db.scannedProductDao()

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(surahNumber: Int, verseNumber: Int) {
        bookmarkDao.deleteBookmark(surahNumber, verseNumber)
    }

    fun isBookmarked(surahNumber: Int, verseNumber: Int): Flow<Boolean> = 
        bookmarkDao.isBookmarked(surahNumber, verseNumber)

    fun getAllCachedNews(): Flow<List<NewsEntity>> = newsDao.getAllNews()

    suspend fun cacheNews(newsList: List<NewsEntity>) {
        newsDao.insertNews(newsList)
    }

    suspend fun clearNews() {
        newsDao.clearAllNews()
    }

    fun getAllScannedProducts(): Flow<List<ScannedProductEntity>> = 
        scannedProductDao.getAllScannedProducts()

    suspend fun insertScannedProduct(product: ScannedProductEntity) {
        scannedProductDao.insertProduct(product)
    }

    suspend fun deleteScannedProduct(barcode: String) {
        scannedProductDao.deleteProduct(barcode)
    }

    suspend fun clearScannedHistory() {
        scannedProductDao.clearHistory()
    }

    suspend fun getProductByBarcode(barcode: String): ScannedProductEntity? =
        scannedProductDao.getProductByBarcode(barcode)
}

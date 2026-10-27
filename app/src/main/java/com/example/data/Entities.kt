package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahNumber: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseArabic: String,
    val verseEnglish: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val source: String,
    val category: String,
    val publishDate: String,
    val imageUrl: String
)

@Entity(tableName = "scanned_products")
data class ScannedProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String,
    val status: String, // "HALAL", "HARAM", "DOUBTFUL"
    val reason: String, // Detailed explanation
    val ingredients: String, // Stringified list or raw text
    val flaggedIngredients: String, // Comma-separated list of flagged ingredients
    val timestamp: Long = System.currentTimeMillis()
)

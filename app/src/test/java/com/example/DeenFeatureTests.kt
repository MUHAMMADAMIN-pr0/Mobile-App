package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.QuranData
import com.example.data.ScannedProductEntity
import com.example.utils.LunarCalculator
import com.example.utils.PrayerTimeCalculator
import com.example.viewmodel.DeenViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeenFeatureTests {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `test PrayerTimeCalculator computes valid schedules`() {
        val schedule = PrayerTimeCalculator.calculatePrayerTimes(
            lat = 21.3891, // Makkah
            lng = 39.8579,
            timezoneOffset = 3.0,
            date = Date(),
            method = PrayerTimeCalculator.CalculationMethod.MWL,
            asrMethod = PrayerTimeCalculator.AsrMethod.STANDARD
        )

        assertNotNull(schedule)
        assertTrue(schedule.fajr.isNotEmpty())
        assertTrue(schedule.dhuhr.isNotEmpty())
        assertTrue(schedule.asr.isNotEmpty())
        assertTrue(schedule.maghrib.isNotEmpty())
        assertTrue(schedule.isha.isNotEmpty())
    }

    @Test
    fun `test LunarCalculator outputs valid Hijri date and Moon phase`() {
        val hijriDate = LunarCalculator.getHijriDate(Date())
        assertNotNull(hijriDate)
        assertTrue(hijriDate.year > 1400)
        assertTrue(hijriDate.monthName.isNotEmpty())
        assertTrue(hijriDate.day in 1..30)

        val phase = LunarCalculator.getMoonPhase(Date())
        assertNotNull(phase)
        assertTrue(phase.phaseName.isNotEmpty())
        assertTrue(phase.illuminatedFraction in 0f..1f)
    }

    @Test
    fun `test QuranData contains essential surahs and verses`() {
        val surahs = QuranData.surahs
        assertFalse(surahs.isEmpty())
        
        val fatihah = surahs.find { it.number == 1 }
        assertNotNull(fatihah)
        assertEquals("Al-Fatihah", fatihah?.englishName)
        assertEquals(7, fatihah?.verses?.size)

        val kahf = surahs.find { it.number == 18 }
        assertNotNull(kahf)
        assertEquals("Al-Kahf", kahf?.englishName)
    }

    @Test
    fun `test Room Database inserts and retrieves bookmarks`() = runBlocking {
        val bookmarkDao = database.bookmarkDao()
        val bookmark = BookmarkEntity(
            surahNumber = 1,
            surahName = "Al-Fatihah",
            verseNumber = 1,
            verseArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            verseEnglish = "In the name of Allah",
            timestamp = System.currentTimeMillis()
        )

        bookmarkDao.insertBookmark(bookmark)
        val allBookmarks = bookmarkDao.getAllBookmarks().first()

        assertEquals(1, allBookmarks.size)
        assertEquals("Al-Fatihah", allBookmarks[0].surahName)

        bookmarkDao.deleteBookmark(1, 1)
        val afterDelete = bookmarkDao.getAllBookmarks().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `test Room Database inserts and retrieves scanned products`() = runBlocking {
        val productDao = database.scannedProductDao()
        val scanned = ScannedProductEntity(
            barcode = "041220032549",
            name = "Organic Cookies",
            brand = "Bakery",
            status = "HALAL",
            reason = "100% plant-based ingredients",
            ingredients = "Flour, Sugar, Cocoa",
            flaggedIngredients = "",
            timestamp = System.currentTimeMillis()
        )

        productDao.insertProduct(scanned)
        val products = productDao.getAllScannedProducts().first()

        assertEquals(1, products.size)
        assertEquals("Organic Cookies", products[0].name)
        assertEquals("HALAL", products[0].status)
    }

    @Test
    fun `test DeenViewModel initial state and city selection`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = DeenViewModel(app)

        assertEquals("Makkah (Mecca)", viewModel.selectedCity.value.name)
        assertTrue(viewModel.cities.size >= 5)

        val newCity = viewModel.cities.first { it.name.contains("London") }
        viewModel.selectCity(newCity)
        assertEquals("London", viewModel.selectedCity.value.name)
    }
}

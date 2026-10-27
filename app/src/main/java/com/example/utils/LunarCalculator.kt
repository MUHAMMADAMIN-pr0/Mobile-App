package com.example.utils

import java.util.Calendar
import java.util.Date
import kotlin.math.floor

object LunarCalculator {

    data class HijriDate(
        val year: Int,
        val monthName: String,
        val monthNumber: Int, // 1-indexed
        val day: Int
    )

    private val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' ath-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah"
    )

    /**
     * Converts a Gregorian date into a Hijri date using a tabular calendar algorithm.
     */
    fun getHijriDate(date: Date): HijriDate {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Day Number calculation
        var y = year
        var m = month
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5

        // Hijri conversion from Julian Day
        val epochJd = 1948439.5 // JDN of Islamic epoch (July 16, 622 CE)
        val diff = jd - epochJd

        // Estimate Hijri years (average Islamic year is 354.367 days)
        val islamicYear = floor(diff / 354.367).toInt()
        
        // Calculate JD at start of estimated Hijri year
        var startJd = getIslamicJd(islamicYear)
        var finalYear = islamicYear
        
        // Adjust year estimate if necessary
        if (jd < startJd) {
            finalYear -= 1
            startJd = getIslamicJd(finalYear)
        } else {
            val nextStartJd = getIslamicJd(finalYear + 1)
            if (jd >= nextStartJd) {
                finalYear += 1
                startJd = nextStartJd
            }
        }

        val daysIntoYear = (jd - startJd).toInt()
        
        // Calculate Hijri month and day
        // Tabular calendar months: alternating 30 and 29 days
        var daysCount = 0
        var finalMonth = 1
        var finalDay = 1
        
        for (mIndex in 1..12) {
            // Dhu al-Hijjah (12th month) has 30 days in leap years, 29 in regular years
            val isLeap = isIslamicLeapYear(finalYear)
            val monthDays = if (mIndex == 12) {
                if (isLeap) 30 else 29
            } else {
                if (mIndex % 2 == 1) 30 else 29
            }
            
            if (daysIntoYear < daysCount + monthDays) {
                finalMonth = mIndex
                finalDay = daysIntoYear - daysCount + 1
                break
            }
            daysCount += monthDays
        }

        // Avoid index bounds just in case of edge conditions
        val monthIndex = (finalMonth - 1).coerceIn(0, 11)
        val monthName = hijriMonths[monthIndex]

        return HijriDate(
            year = finalYear,
            monthName = monthName,
            monthNumber = finalMonth,
            day = finalDay
        )
    }

    private fun isIslamicLeapYear(year: Int): Boolean {
        // Leap years in 30-year cycle: 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29
        val cycleYear = ((year % 30) + 30) % 30
        return cycleYear == 2 || cycleYear == 5 || cycleYear == 7 || cycleYear == 10 ||
               cycleYear == 13 || cycleYear == 16 || cycleYear == 18 || cycleYear == 21 ||
               cycleYear == 24 || cycleYear == 26 || cycleYear == 29
    }

    private fun getIslamicJd(year: Int): Double {
        // Tabular calendar formula to get JD at the start of Hijri year
        val epochJd = 1948439.5
        val yearsElapsed = year - 1
        val cycles = yearsElapsed / 30
        val yearsInCycle = yearsElapsed % 30
        
        var leapDaysInCycle = 0
        for (y in 1..yearsInCycle) {
            if (isIslamicLeapYear(y)) leapDaysInCycle++
        }
        
        val totalDays = cycles * 10631 + yearsInCycle * 354 + leapDaysInCycle
        return epochJd + totalDays
    }

    /**
     * Calculates the Moon Phase from a given Date.
     * Returns a float value representing the phase (0.0 to 1.0) and a name.
     * 0.0 & 1.0 = New Moon, 0.5 = Full Moon.
     */
    data class MoonPhase(
        val value: Float, // 0.0 to 1.0
        val illuminatedFraction: Float, // 0.0 (New Moon) to 1.0 (Full Moon)
        val phaseName: String,
        val isWaxing: Boolean
    )

    fun getMoonPhase(date: Date): MoonPhase {
        // Known New Moon: Jan 6, 2000, 18:14 UTC -> Julian Date: 2451550.26
        val timeMs = date.time
        val jd = (timeMs / 86400000.0) + 2440587.5
        val knownNewMoonJd = 2451550.26
        val synodicMonth = 29.530588853
        
        val elapsedDays = jd - knownNewMoonJd
        val phaseCycles = elapsedDays / synodicMonth
        val currentCyclePart = phaseCycles - floor(phaseCycles)
        
        val ageDays = currentCyclePart * synodicMonth
        
        val phaseName: String
        val illuminated: Float
        val isWaxing: Boolean

        when {
            ageDays < 1.845 -> {
                phaseName = "New Moon"
                illuminated = 0.0f
                isWaxing = true
            }
            ageDays < 5.537 -> {
                phaseName = "Waxing Crescent"
                illuminated = (ageDays / 7.38).toFloat() * 0.5f
                isWaxing = true
            }
            ageDays < 9.228 -> {
                phaseName = "First Quarter"
                illuminated = 0.5f
                isWaxing = true
            }
            ageDays < 12.920 -> {
                phaseName = "Waxing Gibbous"
                illuminated = 0.5f + ((ageDays - 7.38) / 7.38).toFloat() * 0.5f
                isWaxing = true
            }
            ageDays < 16.611 -> {
                phaseName = "Full Moon"
                illuminated = 1.0f
                isWaxing = false
            }
            ageDays < 20.302 -> {
                phaseName = "Waning Gibbous"
                illuminated = 1.0f - ((ageDays - 14.77) / 7.38).toFloat() * 0.5f
                isWaxing = false
            }
            ageDays < 23.994 -> {
                phaseName = "Third Quarter"
                illuminated = 0.5f
                isWaxing = false
            }
            ageDays < 27.685 -> {
                phaseName = "Waning Crescent"
                illuminated = 0.5f - ((ageDays - 22.15) / 7.38).toFloat() * 0.5f
                isWaxing = false
            }
            else -> {
                phaseName = "New Moon"
                illuminated = 0.0f
                isWaxing = true
            }
        }

        return MoonPhase(
            value = currentCyclePart.toFloat(),
            illuminatedFraction = illuminated.coerceIn(0.0f, 1.0f),
            phaseName = phaseName,
            isWaxing = isWaxing
        )
    }
}

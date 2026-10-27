package com.example.utils

import java.util.Calendar
import java.util.Date
import kotlin.math.*

object PrayerTimeCalculator {

    enum class CalculationMethod(val desc: String, val fajrAngle: Double, val ishaAngle: Double, val ishaIntervalMin: Int = 0) {
        MWL("Muslim World League", 18.0, 17.0),
        ISNA("Islamic Society of North America", 15.0, 15.0),
        EGYPT("Egyptian General Authority", 19.5, 17.5),
        UMM_AL_QURA("Umm Al-Qura (Makkah)", 18.5, 0.0, 90), // Isha is 90 mins after Maghrib
        KARACHI("Univ. of Islamic Sciences, Karachi", 18.0, 18.0)
    }

    enum class AsrMethod(val desc: String, val shadowFactor: Double) {
        STANDARD("Standard (Shafi'i, Maliki, Hanbali)", 1.0),
        HANAFI("Hanafi", 2.0)
    }

    data class PrayerSchedule(
        val dateString: String,
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
        val nextPrayerName: String = "",
        val nextPrayerTimeLeft: String = ""
    )

    private fun dtr(d: Double): Double = d * PI / 180.0
    private fun rtd(r: Double): Double = r * 180.0 / PI

    private fun fixHour(h: Double): Double {
        var x = h
        while (x < 0) x += 24.0
        while (x >= 24) x -= 24.0
        return x
    }

    private fun fixAngle(a: Double): Double {
        var x = a
        while (x < 0) x += 360.0
        while (x >= 360) x -= 360.0
        return x
    }

    /**
     * Main prayer time calculation function
     */
    fun calculatePrayerTimes(
        lat: Double,
        lng: Double,
        timezoneOffset: Double, // in hours, e.g. 1.0 or -5.0
        date: Date,
        method: CalculationMethod = CalculationMethod.MWL,
        asrMethod: AsrMethod = AsrMethod.STANDARD
    ): PrayerSchedule {
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Calculate Julian Day
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = floor(y / 100.0)
        val B = 2.0 - A + floor(A / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5

        // Solar parameters
        val D = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * D)
        val q = fixAngle(280.459 + 0.98564736 * D)
        val L = fixAngle(q + 1.915 * sin(dtr(g)) + 0.020 * sin(dtr(2.0 * g)))
        
        val obliq = 23.439 - 0.00000036 * D
        val decl = rtd(asin(sin(dtr(obliq)) * sin(dtr(L))))
        
        val RA = rtd(atan2(cos(dtr(obliq)) * sin(dtr(L)), cos(dtr(L)))) / 15.0
        val eqt = q / 15.0 - fixHour(RA)

        // Midday (Dhuhr) base
        val baseDhuhr = 12.0 + (timezoneOffset - lng / 15.0) - eqt
        val dhuhrHour = fixHour(baseDhuhr)

        // Helper function for Hour Angle (H)
        fun hourAngle(angle: Double, isSunriseOrSunset: Boolean = false): Double {
            val angleRad = dtr(if (isSunriseOrSunset) -0.833 else -angle)
            val latRad = dtr(lat)
            val declRad = dtr(decl)
            val cosH = (sin(angleRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
            if (cosH < -1.0 || cosH > 1.0) return 0.0 // extreme latitude fallback
            return rtd(acos(cosH)) / 15.0
        }

        // Calculate other prayer hour offsets from Dhuhr
        val fajrHA = hourAngle(method.fajrAngle)
        val sunriseHA = hourAngle(0.833, true)
        val sunsetHA = hourAngle(0.833, true)

        val fajrHour = fixHour(dhuhrHour - fajrHA)
        val sunriseHour = fixHour(dhuhrHour - sunriseHA)
        val maghribHour = fixHour(dhuhrHour + sunsetHA)

        val ishaHour = if (method.ishaIntervalMin > 0) {
            fixHour(maghribHour + (method.ishaIntervalMin / 60.0))
        } else {
            val ishaHA = hourAngle(method.ishaAngle)
            fixHour(dhuhrHour + ishaHA)
        }

        // Asr Calculation
        val diffLatDecl = abs(lat - decl)
        val acotAsrShadow = acot(asrMethod.shadowFactor + tan(dtr(diffLatDecl)))
        val asrAngle = rtd(acotAsrShadow)
        
        val latRad = dtr(lat)
        val declRad = dtr(decl)
        val cosAsrHA = (sin(dtr(asrAngle)) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        val asrHA = if (cosAsrHA in -1.0..1.0) rtd(acos(cosAsrHA)) / 15.0 else 3.0 // safety fallback
        val asrHour = fixHour(dhuhrHour + asrHA)

        // Format hour to HH:MM format
        fun formatTime(h: Double): String {
            val totalMinutes = round(h * 60.0).toInt()
            val hrs = (totalMinutes / 60) % 24
            val mins = totalMinutes % 60
            return String.format("%02d:%02d", hrs, mins)
        }

        val dateStr = String.format("%04d-%02d-%02d", year, month, day)

        // Create initial schedule
        var schedule = PrayerSchedule(
            dateString = dateStr,
            fajr = formatTime(fajrHour),
            sunrise = formatTime(sunriseHour),
            dhuhr = formatTime(dhuhrHour),
            asr = formatTime(asrHour),
            maghrib = formatTime(maghribHour),
            isha = formatTime(ishaHour)
        )

        // Calculate next prayer and countdown
        val currentCal = Calendar.getInstance()
        val curHour = currentCal.get(Calendar.HOUR_OF_DAY)
        val curMin = currentCal.get(Calendar.MINUTE)
        val curInMinutes = curHour * 60 + curMin

        fun toMin(timeStr: String): Int {
            val parts = timeStr.split(":")
            if (parts.size < 2) return 0
            return parts[0].toInt() * 60 + parts[1].toInt()
        }

        val fMins = toMin(schedule.fajr)
        val sMins = toMin(schedule.sunrise)
        val dMins = toMin(schedule.dhuhr)
        val aMins = toMin(schedule.asr)
        val mMins = toMin(schedule.maghrib)
        val iMins = toMin(schedule.isha)

        val nextPrayerName: String
        val diffMin: Int

        when {
            curInMinutes < fMins -> {
                nextPrayerName = "Fajr"
                diffMin = fMins - curInMinutes
            }
            curInMinutes < sMins -> {
                nextPrayerName = "Sunrise"
                diffMin = sMins - curInMinutes
            }
            curInMinutes < dMins -> {
                nextPrayerName = "Dhuhr"
                diffMin = dMins - curInMinutes
            }
            curInMinutes < aMins -> {
                nextPrayerName = "Asr"
                diffMin = aMins - curInMinutes
            }
            curInMinutes < mMins -> {
                nextPrayerName = "Maghrib"
                diffMin = mMins - curInMinutes
            }
            curInMinutes < iMins -> {
                nextPrayerName = "Isha"
                diffMin = iMins - curInMinutes
            }
            else -> {
                nextPrayerName = "Fajr (Tomorrow)"
                diffMin = (1440 - curInMinutes) + fMins
            }
        }

        val hDiff = diffMin / 60
        val mDiff = diffMin % 60
        val timeLeft = if (hDiff > 0) "${hDiff}h ${mDiff}m" else "${mDiff}m"

        return schedule.copy(
            nextPrayerName = nextPrayerName,
            nextPrayerTimeLeft = timeLeft
        )
    }

    private fun acot(x: Double): Double = atan(1.0 / x)

    /**
     * Calculates the exact Qibla bearing relative to true North in degrees (0 to 360).
     */
    fun calculateQiblaDirection(lat: Double, lng: Double): Double {
        val latK = dtr(21.4225) // Official Kaaba Latitude
        val lngK = dtr(39.8262) // Official Kaaba Longitude
        val latU = dtr(lat)
        val lngU = dtr(lng)

        val dLng = lngK - lngU
        val numerator = sin(dLng)
        val denominator = cos(latU) * tan(latK) - sin(latU) * cos(dLng)

        val qiblaRad = atan2(numerator, denominator)
        return (rtd(qiblaRad) + 360.0) % 360.0
    }

    /**
     * Calculates the great-circle distance to Mecca (Kaaba) in kilometers using Haversine.
     */
    fun calculateKaabaDistance(lat: Double, lng: Double): Double {
        val r = 6371.0 // Earth radius in km
        val latK = dtr(21.4225)
        val lngK = dtr(39.8262)
        val latU = dtr(lat)
        val lngU = dtr(lng)

        val dLat = latK - latU
        val dLng = lngK - lngU

        val a = sin(dLat / 2).pow(2) + cos(latU) * cos(latK) * sin(dLng / 2).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }
}

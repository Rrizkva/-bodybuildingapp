package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val dayOfWeekName: String,
    val monthName: String
) {
    /**
     * e.g. "۱۸ آبan ۱۴۰۵"
     */
    fun toFormattedString(): String {
        return "${JalaliDateUtil.toPersianDigits(day)} $monthName ${JalaliDateUtil.toPersianDigits(year)}"
    }

    /**
     * e.g. "یکشنبه، ۱۸ آبان ۱۴۰۵"
     */
    fun toFullFormattedString(): String {
        return "$dayOfWeekName، ${JalaliDateUtil.toPersianDigits(day)} $monthName ${JalaliDateUtil.toPersianDigits(year)}"
    }
}

object JalaliDateUtil {

    private val PERSIAN_MONTHS = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_DAYS_OF_WEEK = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    private val PERSIAN_ORDINALS = arrayOf(
        "اول", "دوم", "سوم", "چهارم", "پنجم",
        "ششم", "هفتم", "هشتم", "نهم", "دهم",
        "یازدهم", "دوازدهم", "سیزدهم", "چهاردهم", "پانزدهم"
    )

    fun getPersianDayOrdinal(index: Int): String {
        return if (index in 1..PERSIAN_ORDINALS.size) {
            "روز ${PERSIAN_ORDINALS[index - 1]}"
        } else {
            "روز ${toPersianDigits(index)}"
        }
    }

    fun fromEpochMillis(epochMillis: Long): JalaliDate {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = epochMillis

        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1 // 1-12
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday (Calendar.SUNDAY is 1)

        val dayOfWeekName = if (dayOfWeekIndex in PERSIAN_DAYS_OF_WEEK.indices) {
            PERSIAN_DAYS_OF_WEEK[dayOfWeekIndex]
        } else {
            ""
        }

        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        val monthName = if (jm in 1..12) PERSIAN_MONTHS[jm - 1] else ""

        return JalaliDate(
            year = jy,
            month = jm,
            day = jd,
            dayOfWeekName = dayOfWeekName,
            monthName = monthName
        )
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) (gy + 1) else gy
        var gDays = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (gDays / 12053))
        gDays %= 12053
        jy += 4 * (gDays / 1461)
        gDays %= 1461
        if (gDays > 365) {
            jy += (gDays - 1) / 365
            gDays = (gDays - 1) % 365
        }
        val jm = if (gDays < 186) 1 + (gDays / 31) else 7 + ((gDays - 186) / 30)
        val jd = 1 + if (gDays < 186) (gDays % 31) else ((gDays - 186) % 30)
        return Triple(jy, jm, jd)
    }

    fun toPersianDigits(input: Any?): String {
        if (input == null) return ""
        val str = when (input) {
            is Double -> {
                if (input % 1.0 == 0.0) input.toLong().toString()
                else String.format(Locale.US, "%.1f", input)
            }
            is Float -> {
                if (input % 1.0f == 0.0f) input.toLong().toString()
                else String.format(Locale.US, "%.1f", input)
            }
            else -> input.toString()
        }

        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else if (ch == '.') {
                sb.append('.')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}

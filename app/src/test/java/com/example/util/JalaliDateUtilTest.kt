package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateUtilTest {

    @Test
    fun toPersianDigits_convertsStandardDigitsCorrectly() {
        assertEquals("۰", JalaliDateUtil.toPersianDigits(0))
        assertEquals("۱۲۳۴۵۶۷۸۹", JalaliDateUtil.toPersianDigits("123456789"))
        assertEquals("۱۲", JalaliDateUtil.toPersianDigits(12))
        assertEquals("۶۲.۵", JalaliDateUtil.toPersianDigits(62.5))
        assertEquals("۱۰۰", JalaliDateUtil.toPersianDigits(100.0))
    }

    @Test
    fun gregorianToJalali_convertsStandardDatesAccurately() {
        // 2026-03-21 -> 1405 Farvardin 1 (Nowruz)
        val (y1, m1, d1) = JalaliDateUtil.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, y1)
        assertEquals(1, m1)
        assertEquals(1, d1)

        // 2026-11-08 -> 1405 Aban 17
        val (y2, m2, d2) = JalaliDateUtil.gregorianToJalali(2026, 11, 8)
        assertEquals(1405, y2)
        assertEquals(8, m2)
        assertTrue(d2 in 16..18)
    }

    @Test
    fun getPersianDayOrdinal_returnsCorrectPersianName() {
        assertEquals("روز اول", JalaliDateUtil.getPersianDayOrdinal(1))
        assertEquals("روز دوم", JalaliDateUtil.getPersianDayOrdinal(2))
        assertEquals("روز سوم", JalaliDateUtil.getPersianDayOrdinal(3))
        assertEquals("روز چهارم", JalaliDateUtil.getPersianDayOrdinal(4))
        assertEquals("روز دهم", JalaliDateUtil.getPersianDayOrdinal(10))
    }
}

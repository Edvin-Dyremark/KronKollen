package com.kronkollen

import com.kronkollen.util.DateFormatOption
import com.kronkollen.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DatesTest {

    @Test
    fun parsesIso() {
        assertEquals(LocalDate.of(2026, 5, 31), Dates.parse("2026-05-31", DateFormatOption.AUTO))
    }

    @Test
    fun parsesDottedEuropean() {
        assertEquals(LocalDate.of(2026, 5, 31), Dates.parse("31.05.2026", DateFormatOption.DMY_DOT))
    }

    @Test
    fun parsesExcelSerial() {
        // 2026-05-31 is serial 46173 under the 1900 date system.
        assertEquals(LocalDate.of(2026, 5, 31), Dates.parse("46173", DateFormatOption.EXCEL_SERIAL))
    }

    @Test
    fun autoDetectsExcelSerial() {
        assertEquals(LocalDate.of(2026, 5, 31), Dates.parse("46173", DateFormatOption.AUTO))
    }

    @Test
    fun returnsNullForGarbage() {
        assertNull(Dates.parse("hello", DateFormatOption.AUTO))
    }
}

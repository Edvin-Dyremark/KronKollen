package com.kronkollen

import com.kronkollen.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parsesSwedishFormat() {
        assertEquals(-123456L, Money.parseToOre("-1 234,56"))
    }

    @Test
    fun parsesEnglishFormat() {
        assertEquals(123456L, Money.parseToOre("1,234.56"))
    }

    @Test
    fun parsesPlainNumberWithKrSuffix() {
        assertEquals(-5000L, Money.parseToOre("-50,00 kr"))
    }

    @Test
    fun parsesParenthesisedNegative() {
        assertEquals(-10000L, Money.parseToOre("(100,00)"))
    }

    @Test
    fun returnsNullForNonNumber() {
        assertNull(Money.parseToOre("inte ett tal"))
    }

    @Test
    fun formatsSwedishCurrency() {
        assertEquals("-1 234,56 kr", Money.format(-123456L))
        assertEquals("0,00 kr", Money.format(0L))
        assertEquals("42,50 kr", Money.format(4250L))
    }
}

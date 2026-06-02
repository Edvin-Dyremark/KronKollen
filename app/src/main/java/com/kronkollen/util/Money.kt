package com.kronkollen.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Money is stored everywhere as [Long] *öre* (1/100 kr) to avoid floating-point drift.
 * Negative = money out (an expense), positive = money in.
 */
object Money {

    private const val NBSP = ' ' // non-breaking space, Swedish thousands separator

    /** Format öre as Swedish currency, e.g. -123456 -> "-1 234,56 kr". */
    fun format(ore: Long, withSuffix: Boolean = true): String {
        val negative = ore < 0
        val abs = abs(ore)
        val kronor = abs / 100
        val cents = (abs % 100).toInt()
        val grouped = groupThousands(kronor)
        val sign = if (negative) "-" else ""
        val suffix = if (withSuffix) " kr" else ""
        return "$sign$grouped,${cents.toString().padStart(2, '0')}$suffix"
    }

    /** Compact form for axis labels, e.g. 1 234 kr (no decimals). */
    fun formatWhole(ore: Long): String {
        val negative = ore < 0
        val kronor = abs(ore) / 100
        val sign = if (negative) "-" else ""
        return "$sign${groupThousands(kronor)} kr"
    }

    private fun groupThousands(value: Long): String {
        val digits = value.toString()
        val sb = StringBuilder()
        for ((index, c) in digits.withIndex()) {
            if (index > 0 && (digits.length - index) % 3 == 0) sb.append(NBSP)
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Parse a bank amount cell into öre. Handles Swedish "1 234,56", English
     * "1,234.56", plain "-1234.56", spaces/non-breaking spaces and a trailing/leading
     * "kr". Returns null when the text is not a number.
     */
    fun parseToOre(raw: String): Long? {
        var s = raw.trim()
            .replace("kr", "", ignoreCase = true)
            .replace(NBSP.toString(), "")
            .replace(" ", "")
            .replace("−", "-") // unicode minus
        if (s.isEmpty()) return null

        // Parenthesised negatives, e.g. (1 234,56)
        var negative = false
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true
            s = s.substring(1, s.length - 1)
        }

        val hasComma = s.contains(',')
        val hasDot = s.contains('.')
        s = when {
            hasComma && hasDot -> {
                // The last separator is the decimal one; the other groups thousands.
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                    s.replace(".", "").replace(',', '.')
                } else {
                    s.replace(",", "")
                }
            }
            hasComma -> s.replace(',', '.')
            else -> s
        }

        return try {
            val negFromSign = s.startsWith("-")
            val value = BigDecimal(s).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
            if (negative && !negFromSign) -value else value
        } catch (e: NumberFormatException) {
            null
        }
    }
}

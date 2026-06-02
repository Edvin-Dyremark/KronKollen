package com.kronkollen

import com.kronkollen.categorize.KeywordMatcher
import com.kronkollen.categorize.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeywordMatcherTest {

    private val rules = listOf(
        Rule("ica", 1),
        Rule("ica maxi", 2),
        Rule("sj", 3),
    )

    @Test
    fun longestMatchWins() {
        assertEquals(2L, KeywordMatcher.match("Kortköp ICA MAXI Stormarknad", rules))
    }

    @Test
    fun fallsBackToShorterMatch() {
        assertEquals(1L, KeywordMatcher.match("ICA Nära Sabbatsberg", rules))
    }

    @Test
    fun caseInsensitive() {
        assertEquals(3L, KeywordMatcher.match("biljett sj regional", rules))
    }

    @Test
    fun noMatchReturnsNull() {
        assertNull(KeywordMatcher.match("Swish till Anna", rules))
    }

    @Test
    fun blankKeywordsIgnored() {
        assertNull(KeywordMatcher.match("anything", listOf(Rule("   ", 9))))
    }
}

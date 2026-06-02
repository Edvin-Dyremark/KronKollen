package com.kronkollen.categorize

/** A keyword -> category association, independent of the persistence layer. */
data class Rule(val keyword: String, val categoryId: Long)

/**
 * Pure, side-effect-free matcher. Given a transaction description and the set of keyword
 * rules, returns the category id to assign, or null when nothing matches.
 *
 * Matching is case-insensitive substring containment. When several keywords match, the
 * **longest** keyword wins (the most specific rule), e.g. "ica maxi" beats "ica".
 */
object KeywordMatcher {

    fun match(description: String, rules: List<Rule>): Long? {
        val haystack = description.lowercase()
        var best: Rule? = null
        for (rule in rules) {
            val needle = rule.keyword.lowercase().trim()
            if (needle.isEmpty()) continue
            if (haystack.contains(needle)) {
                if (best == null || needle.length > best!!.keyword.length) {
                    best = rule.copy(keyword = needle)
                }
            }
        }
        return best?.categoryId
    }
}

package com.kronkollen.data

/**
 * Canonical default keyword rules (keyword -> category name) seeded on first run so
 * auto-categorization works out of the box.
 *
 * Matching (see [com.kronkollen.categorize.KeywordMatcher]) is case-insensitive **substring**
 * containment, and when several keywords match a description the **longest** one wins. Keep
 * keywords specific enough that they don't accidentally appear inside unrelated merchant
 * names (e.g. avoid 2–3 letter fragments).
 */
object DefaultKeywords {
    val rules: List<Pair<String, String>> = listOf(
        // Boende
        "hyra" to "Boende",
        "studentbostäder" to "Boende",

        // Abonnemang
        "vattenfall" to "Abonnemang",
        "telia" to "Abonnemang",
        "spotify" to "Abonnemang",
        "netflix" to "Abonnemang",
        "amazon prime" to "Abonnemang",
        "tgtg" to "Abonnemang",
        "google" to "Abonnemang",
        "ingenj" to "Abonnemang",
        "akad.a-kassa" to "Abonnemang",

        // Mat
        "ica" to "Mat",
        "coop" to "Mat",
        "hemköp" to "Mat",
        "hemkop" to "Mat",
        "gross" to "Mat",

        // Restaurang
        "restaurang" to "Restaurang",
        "royalkrog" to "Restaurang",
        "grillen" to "Restaurang",
        "pressbyr" to "Restaurang",
        "burger king" to "Restaurang",
        "zodiaken" to "Restaurang",
        "sektionsfiket" to "Restaurang",
        "studenthuset" to "Restaurang",
        "espresso" to "Restaurang",
        "mcd" to "Restaurang",
        "mcdonald" to "Restaurang",
        "datateknologse" to "Restaurang",
        "cafe" to "Restaurang",

        // Transport
        "sl" to "Transport",
        "sj" to "Transport",
        "trafik" to "Transport",
        "lokaltrafik" to "Transport",
        "okq8" to "Transport",
        "circle k" to "Transport",
        "flix" to "Transport",
        "ryde" to "Transport",
        "voi" to "Transport",

        // Sport
        "sats" to "Sport",
        "gymb" to "Sport",
        "assist" to "Sport",
        "discgolf" to "Sport",
        "skistar" to "Sport",
        "skidresor" to "Sport",
        "padel" to "Sport",
        "löpning" to "Sport",
        "simh" to "Sport",
        "biltema" to "Sport",
        "jarvso" to "Sport",
        "elias johansson lara" to "Sport",

        // Hälsa
        "apotek" to "Hälsa",
        "synsam" to "Hälsa",
        "lensway" to "Hälsa",
        "vallastad" to "Hälsa",

        // Shopping
        "h&m" to "Shopping",
        "webhallen" to "Shopping",
        "power" to "Shopping",
        "proshop" to "Shopping",
        "steam" to "Shopping",
        "boozt" to "Shopping",
        "zalando" to "Shopping",
        "skolyx" to "Shopping",
        "johnhenric" to "Shopping",
    )
}

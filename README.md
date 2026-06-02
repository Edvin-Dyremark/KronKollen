# KronKollen

KronKollen is a local-first Android app for tracking personal expenses across categories
and time — a replacement for a personal expense-tracking Excel workbook. You import an
`.xlsx` exported from your bank, map its columns once, and the app auto-categorizes
transactions using keyword rules you control.

All data stays **on the device** (Room/SQLite). Currency and formatting are **Swedish
(SEK)**.

## Features (v1)

- **Översikt (Overview)** — spending over a selectable period (default: this year), total,
  per-category breakdown with a donut chart and a monthly-spend bar chart. Tap a category
  to drill into its transactions.
- **Transaktioner (Transactions)** — the full table of every transaction (date,
  description, amount, category) with search and category filters. Re-categorize a row and
  the app offers to remember it as a keyword rule.
- **Kategorier (Categories)** — manage categories and the keywords that auto-match them
  (e.g. `ica` → Mat, `sj` → Transport). Longest keyword wins.
- **Importera (Import)** — pick an `.xlsx`, map the date/description/amount columns
  (single signed column or separate in/out), preview the parsed + auto-categorized rows,
  then commit. Duplicates already in the database are skipped, and the latest stored
  transaction date is shown so you know which period to export next.

## Tech

- Kotlin · Jetpack Compose (Material 3) · Navigation Compose
- Room (local DB) · DataStore (remembers your column mapping)
- Lightweight manual DI (`AppContainer` + `Application`) — no annotation-processor DI
- Self-contained `.xlsx` reader (Zip + XmlPullParser, with Excel date-serial detection) —
  no Apache POI / fastexcel (the latter needs `javax.xml.stream`, absent on Android)
- Charts drawn with Compose `Canvas`
- Money stored as integer öre to avoid floating-point drift

## Project layout

```
com.kronkollen
├─ data/{entity,db,repo,prefs}   Room entities, DAOs, repositories, settings
├─ categorize                    KeywordMatcher (pure, unit-tested)
├─ importer                      XlsxReader, ColumnMapping, TransactionParser
├─ di                            AppContainer (manual DI)
├─ ui/{overview,transactions,categories,importflow,components,theme,navigation}
└─ util                          Money (SEK), Dates (Swedish + flexible parsing)
```

## Build

Open the project in Android Studio and run the `app` configuration (minSdk 26). Unit tests
live in `app/src/test` (`KeywordMatcher`, `Money`, `Dates`).

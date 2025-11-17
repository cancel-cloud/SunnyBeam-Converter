# CLAUDE.md - Architektur & Entwicklungs-Hinweise

Dieses Dokument richtet sich an zukünftige Interaktionen mit Claude (oder anderen KI-Assistenten) und Entwickler, die das Projekt erweitern möchten.

## 🏗️ Architektur-Überblick

### Projektstruktur

```
SunnyBeam-Converter/
├── build.gradle.kts              # Gradle-Build-Konfiguration
├── src/main/kotlin/dev/devbrew/solar/
│   ├── Main.kt                   # Haupteinstiegspunkt
│   ├── config/                   # Konfiguration
│   │   └── AppConfig.kt          # CLI-Argumente & Konfiguration
│   ├── model/                    # Datenmodelle
│   │   ├── Models.kt             # Alle Datenklassen
│   │   └── LocalDateSerializer.kt # Serialisierung für JSON
│   ├── input/                    # Dateneingabe
│   │   ├── DataScanner.kt        # Scannt Dateien & gruppiert nach Monat
│   │   └── DailyCsvParser.kt     # Parst SunnyBeam-CSV-Dateien
│   ├── aggregation/              # Datenverarbeitung
│   │   └── MonthlyAggregator.kt  # Aggregiert Tages- zu Monatsdaten
│   ├── export/                   # Datenausgabe
│   │   ├── MonthlyExporter.kt    # Export zu CSV (Matrix & Summary)
│   │   └── DashboardExporter.kt  # Export zu JSON für Dashboard
│   └── util/                     # Hilfsfunktionen
│       ├── Logger.kt             # Einfaches Logging
│       └── FileNameParser.kt     # Parst Dateinamen & Datumsformate
├── web/                          # Frontend
│   ├── index.html                # Dashboard-UI
│   ├── style.css                 # Styling
│   └── app.js                    # Dashboard-Logik
└── data/                         # Beispieldaten & Ausgabe
    ├── [YY-MM]/                  # Optional: Unterordner pro Monat
    │   └── YY-MM-DD.csv          # Tages-CSV-Dateien
    ├── YY_MM.csv                 # Generierte Monats-Matrix
    └── YY-MM-summary.csv         # Generierte Monats-Zusammenfassung
```

## 📦 Haupt-Module & Verantwortlichkeiten

### 1. Main.kt - Orchestrierung
- **Zweck:** Haupteinstiegspunkt, orchestriert den gesamten Ablauf
- **Ablauf:**
  1. Parse CLI-Argumente → `AppConfig`
  2. Scanne Dateien → `DataScanner`
  3. Für jeden Monat:
     - Prüfe, ob Verarbeitung nötig ist
     - Parse Tages-CSV → `DailyCsvParser`
     - Aggregiere Daten → `MonthlyAggregator`
     - Exportiere CSV & JSON → `MonthlyExporter`, `DashboardExporter`

### 2. config/AppConfig.kt - Konfiguration
- **Zweck:** Verwaltet Kommandozeilen-Argumente
- **Unterstützte Parameter:**
  - `--data-dir=<path>`: Datenverzeichnis (Standard: `./data`)
  - `--force`: Erzwinge Neuberechnung aller Monate
  - `--verbose`: Ausführliche Ausgabe
  - `--help`: Hilfe anzeigen

### 3. model/Models.kt - Datenmodelle

Wichtige Klassen:

```kotlin
// Einzelne Messung aus CSV
data class MeasurementRecord(
    val timestamp: LocalDateTime,
    val value: Double?  // null = keine Messung (nachts)
)

// Alle Daten für einen Tag
data class DayData(
    val date: LocalDate,
    val records: List<MeasurementRecord>
) {
    fun calculateDailyTotal(): Double  // last - first reading
    fun firstReading(): Double?
    fun lastReading(): Double?
    fun measurementCount(): Int
}

// Zusammenfassung eines Tages (für Export)
@Serializable
data class DaySummary(
    val date: LocalDate,
    val totalKwh: Double,
    val firstReading: Double?,
    val lastReading: Double?,
    val numMeasurements: Int
)

// Zusammenfassung eines Monats (für Dashboard-JSON)
@Serializable
data class MonthSummary(
    val year: Int,
    val month: Int,
    val label: String,              // "November 2023"
    val summaryFile: String,        // "23-11-summary.csv"
    val matrixFile: String,         // "23_11.csv"
    val days: List<DaySummary>,
    val totalMonthKwh: Double
)

// Root-Struktur für dashboard-data.json
@Serializable
data class DashboardData(
    val months: List<MonthSummary>,
    val generatedAt: String
)
```

### 4. input/DailyCsvParser.kt - CSV-Parsing

**Wichtige Details:**
- Encoding: `ISO-8859-1` (Latin-1)
- Delimiter: Semikolon (`;`)
- Dezimal-Separator: Komma (`,`) → wird zu Punkt (`.`) konvertiert
- Daten beginnen nach Zeile `DD.MM.YYYY HH:mm;kWh`
- Format: `01.11.2023 08:20;46609,111`
- Leere Werte (nachts) werden als `null` behandelt

### 5. aggregation/MonthlyAggregator.kt - Kernlogik

**Wichtige Methoden:**

```kotlin
// Prüft, ob Monat neu verarbeitet werden muss
fun shouldProcessMonth(
    dataDir: File,
    yearMonth: YearMonth,
    dailyFiles: List<File>
): Boolean

// Verarbeitet einen Monat
fun processMonth(
    yearMonth: YearMonth,
    dailyFiles: List<File>
): MonthProcessingResult

// Exportiert Monatsdaten
fun exportMonth(
    dataDir: File,
    monthSummary: MonthSummary,
    dayDataMap: Map<LocalDate, DayData>
)

// Lädt existierende Summary (für Skip-Fall)
fun loadExistingSummary(
    dataDir: File,
    yearMonth: YearMonth
): MonthSummary?
```

**Inkrementelles Verhalten:**
- Ein Monat wird verarbeitet, wenn:
  - Matrix-Datei (`YY_MM.csv`) fehlt, ODER
  - Summary-Datei (`YY-MM-summary.csv`) fehlt, ODER
  - Eine Tages-CSV neuer ist als beide Ausgabedateien
- Mit `--force` werden alle Monate neu verarbeitet

### 6. export/MonthlyExporter.kt - CSV-Export

**Zwei Ausgabeformate:**

1. **Summary-CSV** (`YY-MM-summary.csv`):
   ```csv
   date;total_kwh;first_reading;last_reading;num_measurements
   2023-11-01;12.345;46600.000;46612.345;120
   ```
   - Semikolon-getrennt
   - Dezimalpunkt
   - Eine Zeile pro Tag

2. **Matrix-CSV** (`YY_MM.csv`):
   ```
   Uhrzeit	01.11.2023	02.11.2023	...
   00:00
   00:10
   08:20	46609.111
   ```
   - Tab-getrennt (TSV)
   - Zeilen: Uhrzeiten (00:00 - 23:50, 10-Min-Schritte)
   - Spalten: Tage des Monats
   - Werte: Zählerstände mit Dezimalpunkt

### 7. export/DashboardExporter.kt - JSON-Export

- Erstellt `output/dashboard-data.json`
- Verwendet `kotlinx.serialization`
- Struktur: siehe `DashboardData` Modell
- Pretty-printed für Lesbarkeit

### 8. util/FileNameParser.kt - Hilfsfunktionen

**Wichtige Methoden:**

```kotlin
// Parst Datum aus Dateiname: "23-11-01.csv" → LocalDate
fun parseDate(filename: String): LocalDate?

// Parst Monat aus Ordnername: "23-11" → YearMonth
fun parseYearMonth(folderName: String): YearMonth?

// Generiert Dateinamen
fun getMatrixFileName(yearMonth: YearMonth): String  // "23_11.csv"
fun getSummaryFileName(yearMonth: YearMonth): String // "23-11-summary.csv"

// Formatiert Monat als Label
fun formatMonthLabel(yearMonth: YearMonth): String  // "November 2023"
```

## 🌐 Frontend-Architektur

### web/index.html
- Einfache, semantische HTML-Struktur
- Drei Hauptbereiche:
  1. Globale Statistiken (alle Monate)
  2. Monats-Auswahl
  3. Monats-Details (Chart + Tabelle)

### web/style.css
- CSS-Variablen für einfache Anpassung
- Responsive Design (Mobile-first)
- Card-basiertes Layout
- Sanfte Hover-Effekte

### web/app.js
- Vanilla JavaScript (kein Framework)
- Chart.js für Visualisierung
- Funktionen:
  - `loadDashboardData()`: Lädt JSON
  - `displayDashboard()`: Zeigt Übersicht
  - `displayMonthDetails()`: Zeigt Monatsdetails
  - `updateChart()`: Aktualisiert Chart.js
  - `updateTable()`: Aktualisiert Tabelle

## 🔧 Wie man Änderungen vornimmt

### Neue CLI-Parameter hinzufügen

1. In `config/AppConfig.kt`:
   ```kotlin
   data class AppConfig(
       val dataDir: File,
       val force: Boolean,
       val verbose: Boolean,
       val myNewParam: String  // NEU
   )
   ```

2. In `AppConfig.parse()` den Parameter parsen

3. In `Main.kt` den Parameter verwenden

### Neues Ausgabeformat hinzufügen

1. Erstelle neue Klasse in `export/` (z.B. `XmlExporter.kt`)

2. Registriere in `MonthlyAggregator.exportMonth()`:
   ```kotlin
   fun exportMonth(...) {
       exporter.exportSummary(...)
       exporter.exportMatrixWithTimeData(...)
       xmlExporter.export(...)  // NEU
   }
   ```

### Dashboard erweitern

1. **JSON erweitern:**
   - Füge Felder zu `MonthSummary` oder `DaySummary` hinzu
   - Export in `DashboardExporter.kt` anpassen

2. **Frontend erweitern:**
   - HTML: Neuen Bereich in `index.html` einfügen
   - CSS: Styling in `style.css` ergänzen
   - JS: Neue Funktion in `app.js` schreiben

**Beispiel:** Monats-Vergleich hinzufügen:

```javascript
// In app.js
function displayMonthComparison(months) {
    const ctx = document.getElementById('comparison-chart');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: months.map(m => m.label),
            datasets: [{
                label: 'Monatsertrag',
                data: months.map(m => m.totalMonthKwh)
            }]
        }
    });
}
```

### Neue Statistiken berechnen

1. In `model/DayData.kt` neue Methode hinzufügen:
   ```kotlin
   fun calculatePeakPower(): Double {
       // Berechne Spitzenleistung aus Messungen
   }
   ```

2. In `DaySummary` neues Feld hinzufügen:
   ```kotlin
   @Serializable
   data class DaySummary(
       ...,
       val peakPower: Double?  // NEU
   )
   ```

3. In `MonthData.getDaySummaries()` befüllen

## 🧪 Testing-Strategie

### Manueller Test-Workflow

1. Beispieldaten vorbereiten (bereits in `data/23_11/` und `data/23_12/`)

2. Build & Run:
   ```bash
   ./gradlew clean build
   java -jar build/libs/SunnyBeamAggregator-1.0.0.jar --verbose
   ```

3. Prüfen:
   - CSV-Dateien in `data/` erstellt?
   - JSON in `output/dashboard-data.json` erstellt?
   - Dashboard funktioniert?

4. Inkrementelles Verhalten testen:
   ```bash
   # Erster Lauf
   java -jar ...

   # Zweiter Lauf (sollte skippen)
   java -jar ...

   # Mit Force (sollte neu berechnen)
   java -jar ... --force
   ```

### Unit-Tests schreiben

Beispiel für `FileNameParser`:

```kotlin
// in src/test/kotlin/dev/devbrew/solar/util/FileNameParserTest.kt
class FileNameParserTest {
    @Test
    fun `should parse valid date from filename`() {
        val date = FileNameParser.parseDate("23-11-01.csv")
        assertNotNull(date)
        assertEquals(2023, date!!.year)
        assertEquals(11, date.monthValue)
        assertEquals(1, date.dayOfMonth)
    }
}
```

## 📝 Beispiel-Prompts für zukünftige Claude-Sessions

### Feature-Erweiterungen

> "Bitte erweitere das Dashboard um einen Monats-Vergleich. Zeige ein Liniendiagramm, das die Erträge aller Monate miteinander vergleicht. Die Daten sind bereits in `dashboard-data.json` verfügbar."

> "Füge eine Export-Funktion hinzu, die die Monatsdaten als XML im Format `<month><day date='...'>...</day></month>` exportiert. Erstelle dazu eine neue Klasse `XmlExporter.kt` im `export/` Package."

### Bug-Fixes

> "Die Matrix-Datei enthält falsche Zeitstempel. Bitte prüfe `MonthlyExporter.exportMatrixWithTimeData()` und stelle sicher, dass die Zeitstempel korrekt aus den `MeasurementRecord` Objekten übernommen werden."

### Performance-Optimierungen

> "Die Verarbeitung großer Monate (30+ Tage mit je 144 Messungen) ist langsam. Bitte analysiere `MonthlyAggregator.processMonth()` und optimiere die Datenstrukturen, z.B. durch Verwendung von `Sequence` statt `List` bei der Verarbeitung."

### Dokumentation

> "Erstelle eine detaillierte API-Dokumentation für alle Public-Methoden im `aggregation/` Package. Verwende KDoc-Format und füge Beispiele hinzu."

## 🔍 Wichtige Design-Entscheidungen

### Warum zwei Ausgabeformate (Matrix + Summary)?

- **Matrix:** Kompatibel mit SunnyBeam-Format, für Excel-Analyse geeignet
- **Summary:** Kompakt, für programmatische Verarbeitung optimiert

### Warum Inkrementelles Verhalten?

- Mein Vater soll die Anwendung regelmäßig starten können
- Schnelle Ausführung (nur neue Daten werden verarbeitet)
- Keine versehentliche Überschreibung existierender Daten

### Warum Statisches Dashboard statt Web-App?

- Einfachheit: Keine Server-Verwaltung nötig
- Portabilität: Kann auf jedem Gerät geöffnet werden
- Offline-Fähig: Funktioniert auch ohne Internet

### Warum Kotlin statt Java?

- Moderne Sprachfeatures (Data Classes, Extension Functions)
- Null-Safety
- Bessere Lesbarkeit
- Gute Java-Interoperabilität

## 🚨 Bekannte Einschränkungen

1. **Matrix-Export ohne vollständige Daten:**
   - Wenn nur Summaries geladen werden (Skip-Fall), kann die Matrix nicht neu erstellt werden
   - Lösung: Matrix wird nur bei Neu-Verarbeitung geschrieben

2. **Zeitzonen:**
   - Keine explizite Zeitzonen-Behandlung
   - Annahme: Alle Daten in lokaler Zeit

3. **Fehlerhafte CSV-Dateien:**
   - Fehlerhafte Zeilen werden übersprungen (mit Warning)
   - Keine automatische Korrektur

4. **Dashboard-Performance:**
   - Bei 100+ Monaten könnte das Laden langsam werden
   - Lösung: Pagination oder Lazy-Loading hinzufügen

## 📚 Weiterführende Ressourcen

- **Kotlin Docs:** [https://kotlinlang.org/docs/](https://kotlinlang.org/docs/)
- **Kotlinx Serialization:** [https://github.com/Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Chart.js Docs:** [https://www.chartjs.org/docs/](https://www.chartjs.org/docs/)
- **Gradle Shadow Plugin:** [https://github.com/johnrengelman/shadow](https://github.com/johnrengelman/shadow)

---

**Letzte Aktualisierung:** November 2025
**Maintainer:** DevBrew
**Für Claude:** Dieses Dokument sollte bei jeder größeren Änderung aktualisiert werden.

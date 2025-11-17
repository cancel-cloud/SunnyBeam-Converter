# SunnyBeam Solar Data Aggregator

Eine Kotlin-Anwendung zur Aggregation und Auswertung von SunnyBeam-Solar-CSV-Dateien mit einem übersichtlichen Web-Dashboard.

## ☀️ Überblick

Diese Anwendung liest tägliche CSV-Dateien von Ihrem SunnyBeam-Solarwechselrichter ein und erstellt automatisch:
- Monatliche Übersichtsdateien (Matrix-Format, kompatibel mit SunnyBeam)
- Monatliche Zusammenfassungen mit Tagesstatistiken
- Ein interaktives Web-Dashboard zur Visualisierung der Daten

## 📋 Voraussetzungen

- **Java Runtime Environment (JRE)** Version 21 oder höher
  - Download: [https://adoptium.net/](https://adoptium.net/)
  - Prüfen Sie die Installation mit: `java -version`

## 🚀 Schnellstart für Benutzer

### Schritt 1: Projektstruktur vorbereiten

Erstellen Sie folgende Ordnerstruktur:

```
SunnyBeamAggregator/
├── SunnyBeamAggregator-1.0.0.jar
├── data/
│   ├── 23-11/                    (optional: Unterordner pro Monat)
│   │   ├── 23-11-01.csv
│   │   ├── 23-11-02.csv
│   │   └── ...
│   └── oder direkt hier:
│       ├── 23-11-01.csv
│       ├── 23-11-02.csv
│       └── ...
├── output/                       (wird automatisch erstellt)
└── web/                          (für das Dashboard)
    ├── index.html
    ├── style.css
    └── app.js
```

### Schritt 2: CSV-Dateien hinzufügen

Kopieren Sie Ihre SunnyBeam-CSV-Dateien in den `data/` Ordner. Die Dateien sollten so benannt sein:
- **Format:** `YY-MM-DD.csv` (z.B. `23-11-01.csv` für den 1. November 2023)
- **Optional:** Sie können die Dateien auch in Unterordnern organisieren (z.B. `data/23-11/`)

### Schritt 3: Anwendung starten

#### Option A: Doppelklick (Windows)
Doppelklicken Sie einfach auf die JAR-Datei `SunnyBeamAggregator-1.0.0.jar`.

#### Option B: Kommandozeile
```bash
java -jar SunnyBeamAggregator-1.0.0.jar
```

Die Anwendung verarbeitet automatisch alle Monate und erstellt die Ausgabedateien.

### Schritt 4: Dashboard öffnen

#### Option A: Lokaler Webserver (empfohlen)
```bash
# Im Projektverzeichnis ausführen:
python3 -m http.server 8000

# Oder mit Python 2:
python -m SimpleHTTPServer 8000
```

Dann öffnen Sie im Browser: `http://localhost:8000/web/`

#### Option B: Direkt öffnen
Öffnen Sie die Datei `web/index.html` direkt in Ihrem Browser (funktioniert möglicherweise nicht in allen Browsern wegen CORS-Einschränkungen).

## 📊 Ausgabedateien

Nach der Verarbeitung finden Sie folgende Dateien:

### Im `data/` Ordner:

1. **Monats-Matrix** (`YY_MM.csv`):
   - Tab-getrennte Datei
   - Zeilen: Uhrzeiten (00:00 bis 23:50 in 10-Min-Schritten)
   - Spalten: Tage des Monats
   - Werte: Zählerstände (E-Total) mit Dezimalpunkt

2. **Monats-Zusammenfassung** (`YY-MM-summary.csv`):
   - Semikolon-getrennte Datei
   - Spalten: Datum, Tagesertrag, Erster Wert, Letzter Wert, Anzahl Messungen

### Im `output/` Ordner:

3. **Dashboard-Daten** (`dashboard-data.json`):
   - JSON-Datei mit allen Daten für das Web-Dashboard

## ⚙️ Erweiterte Optionen

### Kommandozeilen-Parameter

```bash
# Anderen Datenordner verwenden
java -jar SunnyBeamAggregator-1.0.0.jar --data-dir=/pfad/zum/ordner

# Alle Monate neu berechnen (auch wenn schon aktuell)
java -jar SunnyBeamAggregator-1.0.0.jar --force

# Ausführliche Ausgabe
java -jar SunnyBeamAggregator-1.0.0.jar --verbose

# Hilfe anzeigen
java -jar SunnyBeamAggregator-1.0.0.jar --help
```

### Inkrementelle Verarbeitung

Die Anwendung ist intelligent:
- Sie verarbeitet nur Monate neu, wenn:
  - Die Ausgabedateien noch nicht existieren, ODER
  - Die CSV-Dateien neuer sind als die Ausgabedateien
- Sie können die Anwendung beliebig oft starten - sie aktualisiert nur, was nötig ist
- Mit `--force` können Sie eine vollständige Neuberechnung erzwingen

## 🔧 Problemlösung

### "No CSV files found"
- Stellen Sie sicher, dass die CSV-Dateien im `data/` Ordner liegen
- Prüfen Sie die Dateinamen (Format: `YY-MM-DD.csv`)

### Dashboard zeigt keine Daten
- Stellen Sie sicher, dass die JAR-Datei ausgeführt wurde
- Prüfen Sie, ob `output/dashboard-data.json` existiert
- Verwenden Sie einen lokalen Webserver (siehe Schritt 4)

### "Java version not compatible"
- Installieren Sie Java 21 oder höher
- Prüfen Sie mit: `java -version`

## 📁 Dateiformat der Eingabedaten

Die Anwendung erwartet SunnyBeam-CSV-Dateien mit folgender Struktur:

```csv
sep=;
Version CSV|Tool SunnyBeam2|...

;SN: 2001122827
;SB 3300
;2001122827
;E-Total
;Counter
DD.MM.YYYY HH:mm;kWh
01.11.2023 08:20;46609,111
01.11.2023 08:30;46609,111
...
```

- **Encoding:** ISO-8859-1 (Latin-1)
- **Trennzeichen:** Semikolon (;)
- **Dezimaltrennzeichen:** Komma (,)
- **Datenbereich:** Beginnt nach der Zeile `DD.MM.YYYY HH:mm;kWh`

---

## 👨‍💻 Für Entwickler

### Projekt bauen

```bash
# Projekt klonen
git clone <repository-url>
cd SunnyBeam-Converter

# Projekt bauen (erstellt Shadow JAR)
./gradlew clean build

# JAR befindet sich dann in:
# build/libs/SunnyBeamAggregator-1.0.0.jar
```

### Projekt-Struktur

```
src/main/kotlin/dev/devbrew/solar/
├── Main.kt                           # Einstiegspunkt
├── config/
│   └── AppConfig.kt                  # Konfiguration & CLI-Parsing
├── model/
│   ├── Models.kt                     # Datenmodelle
│   └── LocalDateSerializer.kt        # JSON-Serialisierung
├── input/
│   ├── DataScanner.kt                # Dateien scannen
│   └── DailyCsvParser.kt             # CSV-Parsing
├── aggregation/
│   └── MonthlyAggregator.kt          # Monats-Aggregation
├── export/
│   ├── MonthlyExporter.kt            # CSV-Export
│   └── DashboardExporter.kt          # JSON-Export
└── util/
    ├── Logger.kt                     # Logging
    └── FileNameParser.kt             # Dateinamen-Parsing
```

### Technologie-Stack

- **Sprache:** Kotlin 2.1.0
- **Build-Tool:** Gradle 8.x mit Shadow-Plugin
- **JVM:** Java 21
- **Dependencies:**
  - Kotlinx Serialization (JSON)
  - JUnit Jupiter (Tests)
- **Frontend:**
  - Vanilla JavaScript (ES6+)
  - Chart.js für Visualisierung
  - CSS3 mit CSS-Variablen

### Tests ausführen

```bash
./gradlew test
```

### Weitere Dokumentation

Siehe `CLAUDE.md` für detaillierte Architektur-Informationen und Hinweise für zukünftige Änderungen.

## 📄 Lizenz

MIT License - siehe LICENSE-Datei

## 🤝 Beitragen

Beiträge sind willkommen! Bitte erstellen Sie ein Issue oder Pull Request.

---

**Version:** 1.0.0
**Autor:** DevBrew
**Letzte Aktualisierung:** November 2025

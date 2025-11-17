package dev.devbrew.solar.aggregation

import dev.devbrew.solar.export.MonthlyExporter
import dev.devbrew.solar.input.DailyCsvParser
import dev.devbrew.solar.model.DaySummary
import dev.devbrew.solar.model.MonthData
import dev.devbrew.solar.model.MonthSummary
import dev.devbrew.solar.util.FileNameParser
import dev.devbrew.solar.util.Logger
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * Aggregates daily CSV data into monthly summaries and matrix files.
 */
class MonthlyAggregator(private val logger: Logger) {

    private val parser = DailyCsvParser(logger)
    private val exporter = MonthlyExporter(logger)

    /**
     * Check if a month should be processed.
     *
     * Returns true if:
     * - Either the matrix or summary file is missing, OR
     * - Any daily CSV is newer than both output files
     */
    fun shouldProcessMonth(dataDir: File, yearMonth: YearMonth, dailyFiles: List<File>): Boolean {
        val matrixFile = File(dataDir, FileNameParser.getMatrixFileName(yearMonth))
        val summaryFile = File(dataDir, FileNameParser.getSummaryFileName(yearMonth))

        // If either output file is missing, we need to process
        if (!matrixFile.exists() || !summaryFile.exists()) {
            logger.debug("Output files missing, processing required")
            return true
        }

        // Check if any daily file is newer than the output files
        val matrixTime = matrixFile.lastModified()
        val summaryTime = summaryFile.lastModified()
        val outputTime = minOf(matrixTime, summaryTime)

        val newerDailyFiles = dailyFiles.filter { it.lastModified() > outputTime }
        if (newerDailyFiles.isNotEmpty()) {
            logger.debug("Found ${newerDailyFiles.size} daily file(s) newer than output files")
            return true
        }

        return false
    }

    /**
     * Result of processing a month, including both the summary and raw day data.
     */
    data class MonthProcessingResult(
        val summary: MonthSummary,
        val dayDataMap: Map<LocalDate, dev.devbrew.solar.model.DayData>
    )

    /**
     * Process a month's worth of daily files and create aggregated data.
     */
    fun processMonth(yearMonth: YearMonth, dailyFiles: List<File>): MonthProcessingResult {
        val dayDataMap = mutableMapOf<LocalDate, dev.devbrew.solar.model.DayData>()

        // Parse each daily file
        dailyFiles.forEach { file ->
            val date = FileNameParser.parseDate(file.name)
            if (date != null) {
                val dayData = parser.parse(file, date)
                if (dayData != null) {
                    dayDataMap[date] = dayData
                    logger.debug("  Parsed ${file.name}: ${dayData.measurementCount()} measurements")
                }
            }
        }

        // Create month data
        val monthData = MonthData(yearMonth, dayDataMap)

        // Generate summaries
        val daySummaries = monthData.getDaySummaries()
        val totalMonthKwh = daySummaries.sumOf { it.totalKwh }

        val summary = MonthSummary(
            year = yearMonth.year,
            month = yearMonth.monthValue,
            label = FileNameParser.formatMonthLabel(yearMonth),
            summaryFile = FileNameParser.getSummaryFileName(yearMonth),
            matrixFile = FileNameParser.getMatrixFileName(yearMonth),
            days = daySummaries,
            totalMonthKwh = totalMonthKwh
        )

        return MonthProcessingResult(summary, dayDataMap)
    }

    /**
     * Export the month's data to CSV files.
     * We need the full dayDataMap to export the time-series matrix properly.
     */
    fun exportMonth(dataDir: File, monthSummary: MonthSummary, dayDataMap: Map<LocalDate, dev.devbrew.solar.model.DayData>) {
        val yearMonth = YearMonth.of(monthSummary.year, monthSummary.month)

        // Export summary CSV
        val summaryFile = File(dataDir, monthSummary.summaryFile)
        exporter.exportSummary(summaryFile, monthSummary.days)
        logger.debug("  Exported summary to ${summaryFile.name}")

        // Export matrix CSV with full time-series data
        val matrixFile = File(dataDir, monthSummary.matrixFile)
        exporter.exportMatrixWithTimeData(matrixFile, yearMonth, dayDataMap)
        logger.debug("  Exported matrix to ${matrixFile.name}")
    }

    /**
     * Load an existing month summary from the summary CSV file.
     * Returns null if the file doesn't exist or can't be parsed.
     */
    fun loadExistingSummary(dataDir: File, yearMonth: YearMonth): MonthSummary? {
        val summaryFile = File(dataDir, FileNameParser.getSummaryFileName(yearMonth))
        if (!summaryFile.exists()) {
            return null
        }

        try {
            val lines = summaryFile.readLines()
            if (lines.size < 2) return null // Need at least header + one data line

            val daySummaries = mutableListOf<DaySummary>()

            // Skip header line
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val parts = line.split(";")
                if (parts.size >= 5) {
                    try {
                        val date = LocalDate.parse(parts[0])
                        val totalKwh = parts[1].toDouble()
                        val firstReading = parts[2].toDoubleOrNull()
                        val lastReading = parts[3].toDoubleOrNull()
                        val numMeasurements = parts[4].toInt()

                        daySummaries.add(
                            DaySummary(
                                date = date,
                                totalKwh = totalKwh,
                                firstReading = firstReading,
                                lastReading = lastReading,
                                numMeasurements = numMeasurements
                            )
                        )
                    } catch (e: Exception) {
                        logger.debug("Error parsing summary line: $line")
                    }
                }
            }

            if (daySummaries.isEmpty()) return null

            val totalMonthKwh = daySummaries.sumOf { it.totalKwh }

            return MonthSummary(
                year = yearMonth.year,
                month = yearMonth.monthValue,
                label = FileNameParser.formatMonthLabel(yearMonth),
                summaryFile = FileNameParser.getSummaryFileName(yearMonth),
                matrixFile = FileNameParser.getMatrixFileName(yearMonth),
                days = daySummaries,
                totalMonthKwh = totalMonthKwh
            )
        } catch (e: Exception) {
            logger.debug("Failed to load existing summary: ${e.message}")
            return null
        }
    }
}

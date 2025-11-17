package dev.devbrew.solar.export

import dev.devbrew.solar.model.DaySummary
import dev.devbrew.solar.util.Logger
import java.io.File
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Exports monthly data to CSV files.
 */
class MonthlyExporter(private val logger: Logger) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Export daily summaries to a summary CSV file.
     *
     * Format: date;total_kwh;first_reading;last_reading;num_measurements
     */
    fun exportSummary(file: File, daySummaries: List<DaySummary>) {
        file.bufferedWriter().use { writer ->
            // Write header
            writer.write("date;total_kwh;first_reading;last_reading;num_measurements\n")

            // Write data rows
            daySummaries.sortedBy { it.date }.forEach { summary ->
                writer.write(
                    "${summary.date};" +
                            "${formatDecimal(summary.totalKwh)};" +
                            "${formatDecimal(summary.firstReading)};" +
                            "${formatDecimal(summary.lastReading)};" +
                            "${summary.numMeasurements}\n"
                )
            }
        }
    }

    /**
     * Export a monthly matrix file in SunnyBeam style.
     *
     * Format:
     * - Tab-separated (TSV)
     * - First column: time (HH:mm)
     * - Following columns: one per day in the month
     * - Column headers: dd.MM.yyyy
     * - Values: decimal with dot separator
     */
    fun exportMatrix(file: File, yearMonth: YearMonth, daySummaries: List<DaySummary>) {
        // Create a map from date to day summary for quick lookup
        val summaryMap = daySummaries.associateBy { it.date }

        // We need to create a sparse matrix since we don't have the full measurement data
        // We'll write empty cells for days/times where we don't have data
        // For simplicity, we'll create a minimal matrix with only the dates we have data for

        // Get all dates that have data
        val dates = daySummaries.map { it.date }.sorted()

        if (dates.isEmpty()) {
            logger.warn("No data to export for matrix file")
            return
        }

        // Generate all time slots (00:00 to 23:50 in 10-minute intervals)
        val timeSlots = (0 until 144).map { index ->
            val hour = index / 6
            val minute = (index % 6) * 10
            LocalTime.of(hour, minute)
        }

        file.bufferedWriter().use { writer ->
            // Write header row
            val header = buildString {
                append("Uhrzeit")
                dates.forEach { date ->
                    append("\t")
                    append(date.format(dateFormatter))
                }
            }
            writer.write(header + "\n")

            // Write data rows
            // Note: Since we only have daily summaries and not the full time-series data,
            // we can't fill in the actual values per time slot.
            // This is a limitation - we would need to keep the full DayData to do this properly.
            // For now, we'll create a mostly empty matrix.

            timeSlots.forEach { time ->
                val row = buildString {
                    append(time.format(timeFormatter))
                    dates.forEach { _ ->
                        append("\t")
                        // Empty cell - we don't have time-series data in the summary
                    }
                }
                writer.write(row + "\n")
            }
        }

        logger.warn("Matrix export is limited: full time-series data not available from summaries")
    }

    /**
     * Export a full monthly matrix with actual time-series data.
     * This version requires the full DayData objects, not just summaries.
     */
    fun exportMatrixWithTimeData(
        file: File,
        yearMonth: YearMonth,
        dayDataMap: Map<java.time.LocalDate, dev.devbrew.solar.model.DayData>
    ) {
        // Get all dates in the month (even those without data)
        val daysInMonth = yearMonth.lengthOfMonth()
        val dates = (1..daysInMonth).map { yearMonth.atDay(it) }

        // Generate all time slots
        val timeSlots = (0 until 144).map { index ->
            val hour = index / 6
            val minute = (index % 6) * 10
            LocalTime.of(hour, minute)
        }

        // Build a map of (date, time) -> value
        val dataMatrix = mutableMapOf<Pair<java.time.LocalDate, LocalTime>, Double?>()

        dayDataMap.forEach { (date, dayData) ->
            dayData.records.forEach { record ->
                val time = record.timestamp.toLocalTime()
                dataMatrix[date to time] = record.value
            }
        }

        file.bufferedWriter().use { writer ->
            // Write header row
            val header = buildString {
                append("Uhrzeit")
                dates.forEach { date ->
                    append("\t")
                    append(date.format(dateFormatter))
                }
            }
            writer.write(header + "\n")

            // Write data rows
            timeSlots.forEach { time ->
                val row = buildString {
                    append(time.format(timeFormatter))
                    dates.forEach { date ->
                        append("\t")
                        val value = dataMatrix[date to time]
                        if (value != null) {
                            append(formatDecimal(value))
                        }
                        // else: empty cell
                    }
                }
                writer.write(row + "\n")
            }
        }
    }

    /**
     * Format a decimal number with dot as separator.
     * Null values are formatted as empty string.
     */
    private fun formatDecimal(value: Double?): String {
        return if (value != null) {
            String.format("%.3f", value)
        } else {
            ""
        }
    }
}

package dev.devbrew.solar.util

import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility for parsing dates from file and folder names.
 */
object FileNameParser {
    private val dayFormatter = DateTimeFormatter.ofPattern("yy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yy-MM")

    /**
     * Parse a date from a filename like "23-11-01.csv".
     * Returns null if the filename doesn't match the expected format.
     */
    fun parseDate(filename: String): LocalDate? {
        try {
            val nameWithoutExtension = filename.substringBeforeLast('.')

            // Try YY-MM-DD format
            if (nameWithoutExtension.matches(Regex("\\d{2}-\\d{2}-\\d{2}"))) {
                return LocalDate.parse(nameWithoutExtension, dayFormatter)
            }
        } catch (e: DateTimeParseException) {
            // Ignore and return null
        }
        return null
    }

    /**
     * Parse a year-month from a folder name like "23-11" or "23_11".
     * Returns null if the name doesn't match the expected format.
     */
    fun parseYearMonth(folderName: String): YearMonth? {
        try {
            val normalized = folderName.replace('_', '-')

            if (normalized.matches(Regex("\\d{2}-\\d{2}"))) {
                val date = LocalDate.parse("$normalized-01", dayFormatter)
                return YearMonth.from(date)
            }
        } catch (e: DateTimeParseException) {
            // Ignore and return null
        }
        return null
    }

    /**
     * Generate a matrix filename for a given year-month.
     * Format: YY_MM.csv (e.g., "23_11.csv")
     */
    fun getMatrixFileName(yearMonth: YearMonth): String {
        val yy = (yearMonth.year % 100).toString().padStart(2, '0')
        val mm = yearMonth.monthValue.toString().padStart(2, '0')
        return "${yy}_${mm}.csv"
    }

    /**
     * Generate a summary filename for a given year-month.
     * Format: YY-MM-summary.csv (e.g., "23-11-summary.csv")
     */
    fun getSummaryFileName(yearMonth: YearMonth): String {
        val yy = (yearMonth.year % 100).toString().padStart(2, '0')
        val mm = yearMonth.monthValue.toString().padStart(2, '0')
        return "${yy}-${mm}-summary.csv"
    }

    /**
     * Check if a file is a CSV file (by extension).
     */
    fun isCsvFile(file: File): Boolean {
        return file.isFile && file.extension.lowercase() == "csv"
    }

    /**
     * Format a YearMonth as a readable label.
     * Example: YearMonth.of(2023, 11) -> "November 2023"
     */
    fun formatMonthLabel(yearMonth: YearMonth): String {
        val monthNames = listOf(
            "Januar", "Februar", "März", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Dezember"
        )
        return "${monthNames[yearMonth.monthValue - 1]} ${yearMonth.year}"
    }
}

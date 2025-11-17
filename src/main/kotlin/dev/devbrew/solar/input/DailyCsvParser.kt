package dev.devbrew.solar.input

import dev.devbrew.solar.model.DayData
import dev.devbrew.solar.model.MeasurementRecord
import dev.devbrew.solar.util.Logger
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parser for SunnyBeam daily CSV files.
 *
 * These files have a specific format:
 * - Encoding: ISO-8859-1 (Latin-1)
 * - Delimiter: semicolon (;)
 * - Decimal separator: comma (,)
 * - Header section with metadata (lines starting with ;)
 * - Data section starting after "DD.MM.YYYY HH:mm;kWh"
 */
class DailyCsvParser(private val logger: Logger) {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN)
    private val latin1 = Charset.forName("ISO-8859-1")

    /**
     * Parse a daily CSV file and return DayData.
     *
     * @param file The CSV file to parse
     * @param expectedDate The expected date for this file (used for validation)
     * @return DayData containing all measurements, or null if parsing fails
     */
    fun parse(file: File, expectedDate: LocalDate): DayData? {
        try {
            val lines = file.readLines(latin1)
            val records = mutableListOf<MeasurementRecord>()

            // Skip header lines until we find "DD.MM.YYYY HH:mm;kWh"
            var dataStarted = false

            for ((lineNum, line) in lines.withIndex()) {
                // Skip empty lines
                if (line.isBlank()) continue

                // Check if this is the header line that marks the start of data
                if (line.startsWith("DD.MM.YYYY HH:mm")) {
                    dataStarted = true
                    continue
                }

                // Skip lines before data starts
                if (!dataStarted) continue

                // Parse data lines
                val parts = line.split(";")
                if (parts.size >= 2 && parts[0].isNotBlank()) {
                    try {
                        val timestamp = LocalDateTime.parse(parts[0].trim(), dateTimeFormatter)

                        // Parse the value (may be empty)
                        val value = if (parts[1].trim().isEmpty()) {
                            null
                        } else {
                            // Replace comma with dot for decimal parsing
                            parts[1].trim().replace(",", ".").toDoubleOrNull()
                        }

                        records.add(MeasurementRecord(timestamp, value))

                    } catch (e: Exception) {
                        logger.debug("Error parsing line $lineNum in ${file.name}: $line - ${e.message}")
                        // Continue parsing other lines
                    }
                }
            }

            if (records.isEmpty()) {
                logger.warn("No valid records found in ${file.name}")
                return null
            }

            logger.debug("Parsed ${records.size} records from ${file.name}")
            return DayData(expectedDate, records)

        } catch (e: Exception) {
            logger.error("Failed to parse ${file.name}: ${e.message}")
            return null
        }
    }
}

package dev.devbrew.solar.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * A single measurement record from a daily CSV file.
 */
data class MeasurementRecord(
    val timestamp: LocalDateTime,
    val value: Double?
)

/**
 * All data for a single day.
 */
data class DayData(
    val date: LocalDate,
    val records: List<MeasurementRecord>
) {
    /**
     * Calculate the daily total production.
     * Returns the difference between the last and first non-null reading.
     */
    fun calculateDailyTotal(): Double {
        val validValues = records.mapNotNull { it.value }

        return if (validValues.size >= 2) {
            val first = validValues.first()
            val last = validValues.last()
            maxOf(last - first, 0.0)
        } else {
            0.0
        }
    }

    /**
     * Get the first non-null reading value.
     */
    fun firstReading(): Double? = records.firstOrNull { it.value != null }?.value

    /**
     * Get the last non-null reading value.
     */
    fun lastReading(): Double? = records.lastOrNull { it.value != null }?.value

    /**
     * Count the number of valid measurements.
     */
    fun measurementCount(): Int = records.count { it.value != null }
}

/**
 * Summary statistics for a single day.
 */
@Serializable
data class DaySummary(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val totalKwh: Double,
    val firstReading: Double?,
    val lastReading: Double?,
    val numMeasurements: Int
)

/**
 * All data and summaries for a complete month.
 */
data class MonthData(
    val yearMonth: YearMonth,
    val days: Map<LocalDate, DayData>
) {
    /**
     * Get a sorted list of all days in the month with data.
     */
    fun sortedDays(): List<DayData> = days.values.sortedBy { it.date }

    /**
     * Get day summaries for all days.
     */
    fun getDaySummaries(): List<DaySummary> {
        return sortedDays().map { dayData ->
            DaySummary(
                date = dayData.date,
                totalKwh = dayData.calculateDailyTotal(),
                firstReading = dayData.firstReading(),
                lastReading = dayData.lastReading(),
                numMeasurements = dayData.measurementCount()
            )
        }
    }
}

/**
 * Summary data for a complete month (used for JSON export).
 */
@Serializable
data class MonthSummary(
    val year: Int,
    val month: Int,
    val label: String,
    val summaryFile: String,
    val matrixFile: String,
    val days: List<DaySummary>,
    val totalMonthKwh: Double
)

/**
 * Root structure for the dashboard JSON file.
 */
@Serializable
data class DashboardData(
    val months: List<MonthSummary>,
    val generatedAt: String
)

package dev.devbrew.solar.export

import dev.devbrew.solar.model.DashboardData
import dev.devbrew.solar.model.MonthSummary
import dev.devbrew.solar.util.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Exports dashboard data to JSON format for the web interface.
 */
class DashboardExporter(private val logger: Logger) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Export all month summaries to a dashboard-data.json file.
     */
    fun export(outputDir: File, monthSummaries: List<MonthSummary>) {
        val dashboardData = DashboardData(
            months = monthSummaries.sortedWith(compareBy({ it.year }, { it.month })),
            generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        val jsonString = json.encodeToString(dashboardData)

        val outputFile = File(outputDir, "dashboard-data.json")
        outputFile.writeText(jsonString)

        logger.info("Dashboard data exported to: ${outputFile.absolutePath}")
        logger.debug("Exported ${monthSummaries.size} month(s) with ${monthSummaries.sumOf { it.days.size }} days")
    }
}

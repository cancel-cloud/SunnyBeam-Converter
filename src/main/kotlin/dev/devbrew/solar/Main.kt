package dev.devbrew.solar

import dev.devbrew.solar.aggregation.MonthlyAggregator
import dev.devbrew.solar.config.AppConfig
import dev.devbrew.solar.export.DashboardExporter
import dev.devbrew.solar.input.DataScanner
import dev.devbrew.solar.util.Logger
import java.io.File

/**
 * Main entry point for the SunnyBeam Solar Data Aggregator.
 *
 * This application processes daily CSV files from a SunnyBeam solar system
 * and generates monthly summary files and a web dashboard.
 */
fun main(args: Array<String>) {
    val config = AppConfig.parse(args)
    val logger = Logger(config.verbose)

    logger.info("=================================================")
    logger.info("  SunnyBeam Solar Data Aggregator v1.0.0")
    logger.info("=================================================")
    logger.info("")
    logger.info("Data directory: ${config.dataDir.absolutePath}")
    logger.info("Force regeneration: ${config.force}")
    logger.info("")

    try {
        // Scan for daily CSV files
        logger.info("Scanning for daily CSV files...")
        val scanner = DataScanner(config.dataDir, logger)
        val monthlyGroups = scanner.scanAndGroupByMonth()

        if (monthlyGroups.isEmpty()) {
            logger.warn("No CSV files found in ${config.dataDir.absolutePath}")
            logger.info("Please ensure your CSV files are in one of these formats:")
            logger.info("  - data/YY-MM-DD.csv (e.g., data/23-11-01.csv)")
            logger.info("  - data/YY-MM/YY-MM-DD.csv (e.g., data/23-11/23-11-01.csv)")
            return
        }

        logger.info("Found ${monthlyGroups.size} month(s) with data")
        logger.info("")

        // Process each month
        val aggregator = MonthlyAggregator(logger)
        val allMonthSummaries = mutableListOf<dev.devbrew.solar.model.MonthSummary>()

        monthlyGroups.forEach { (yearMonth, files) ->
            logger.info("Processing ${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}...")

            val shouldProcess = if (config.force) {
                logger.info("  Force mode: regenerating all files")
                true
            } else {
                aggregator.shouldProcessMonth(config.dataDir, yearMonth, files)
            }

            if (shouldProcess) {
                val result = aggregator.processMonth(yearMonth, files)
                aggregator.exportMonth(config.dataDir, result.summary, result.dayDataMap)
                allMonthSummaries.add(result.summary)
                logger.info("  ✓ Month processed successfully")
            } else {
                // Still load the summary for dashboard export
                val monthSummary = aggregator.loadExistingSummary(config.dataDir, yearMonth)
                if (monthSummary != null) {
                    allMonthSummaries.add(monthSummary)
                }
                logger.info("  ⊙ Skipped (already up-to-date)")
            }
            logger.info("")
        }

        // Export dashboard data
        logger.info("Generating dashboard data...")
        val outputDir = File(config.dataDir.parent, "output")
        outputDir.mkdirs()

        val dashboardExporter = DashboardExporter(logger)
        dashboardExporter.export(outputDir, allMonthSummaries)

        logger.info("")
        logger.info("=================================================")
        logger.info("  Processing complete!")
        logger.info("=================================================")
        logger.info("")
        logger.info("Monthly files saved to: ${config.dataDir.absolutePath}")
        logger.info("Dashboard data saved to: ${outputDir.absolutePath}/dashboard-data.json")
        logger.info("")
        logger.info("To view the dashboard:")
        logger.info("  1. Open web/index.html in a web browser")
        logger.info("  2. Or run a local web server in the project root:")
        logger.info("     python3 -m http.server 8000")
        logger.info("     Then open http://localhost:8000/web/")

    } catch (e: Exception) {
        logger.error("Error during processing: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

package dev.devbrew.solar.input

import dev.devbrew.solar.util.FileNameParser
import dev.devbrew.solar.util.Logger
import java.io.File
import java.time.YearMonth

/**
 * Scans the data directory for daily CSV files and groups them by month.
 */
class DataScanner(
    private val dataDir: File,
    private val logger: Logger
) {

    /**
     * Scan the data directory and group CSV files by their year-month.
     *
     * Supports two directory structures:
     * 1. Files directly in data/: data/23-11-01.csv, data/23-11-02.csv, ...
     * 2. Files in month subdirectories: data/23-11/23-11-01.csv, ...
     *
     * Returns a map of YearMonth to List<File>.
     */
    fun scanAndGroupByMonth(): Map<YearMonth, List<File>> {
        val result = mutableMapOf<YearMonth, MutableList<File>>()

        // First, check for CSV files directly in the data directory
        dataDir.listFiles()?.forEach { file ->
            if (FileNameParser.isCsvFile(file)) {
                val date = FileNameParser.parseDate(file.name)
                if (date != null) {
                    val yearMonth = YearMonth.from(date)
                    result.getOrPut(yearMonth) { mutableListOf() }.add(file)
                    logger.debug("Found daily file: ${file.name} -> $yearMonth")
                }
            }
        }

        // Then check for subdirectories that might contain CSV files
        dataDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val yearMonth = FileNameParser.parseYearMonth(dir.name)
                if (yearMonth != null) {
                    dir.listFiles()?.forEach { file ->
                        if (FileNameParser.isCsvFile(file)) {
                            val date = FileNameParser.parseDate(file.name)
                            if (date != null && YearMonth.from(date) == yearMonth) {
                                result.getOrPut(yearMonth) { mutableListOf() }.add(file)
                                logger.debug("Found daily file: ${dir.name}/${file.name} -> $yearMonth")
                            }
                        }
                    }
                }
            }
        }

        // Sort the file lists by filename (which should be chronological)
        result.values.forEach { files ->
            files.sortBy { it.name }
        }

        return result.toSortedMap()
    }
}

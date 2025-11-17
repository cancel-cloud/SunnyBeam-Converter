package dev.devbrew.solar.config

import java.io.File

/**
 * Application configuration parsed from command-line arguments.
 */
data class AppConfig(
    val dataDir: File,
    val force: Boolean,
    val verbose: Boolean
) {
    companion object {
        /**
         * Parse command-line arguments into an AppConfig.
         *
         * Supported arguments:
         * - --data-dir=<path>  : Path to the data directory (default: ./data)
         * - --force            : Force regeneration of all monthly files
         * - --verbose          : Enable verbose logging
         * - --help             : Display help message
         */
        fun parse(args: Array<String>): AppConfig {
            var dataDir = File("./data")
            var force = false
            var verbose = false

            args.forEach { arg ->
                when {
                    arg.startsWith("--data-dir=") -> {
                        val path = arg.substringAfter("--data-dir=")
                        dataDir = File(path)
                    }
                    arg == "--force" -> force = true
                    arg == "--verbose" -> verbose = true
                    arg == "--help" || arg == "-h" -> {
                        printHelp()
                        System.exit(0)
                    }
                    else -> {
                        println("Unknown argument: $arg")
                        println("Use --help for usage information")
                        System.exit(1)
                    }
                }
            }

            // Validate data directory
            if (!dataDir.exists()) {
                println("Error: Data directory does not exist: ${dataDir.absolutePath}")
                println("Please create the directory and add your CSV files.")
                System.exit(1)
            }

            if (!dataDir.isDirectory) {
                println("Error: Data path is not a directory: ${dataDir.absolutePath}")
                System.exit(1)
            }

            return AppConfig(dataDir, force, verbose)
        }

        private fun printHelp() {
            println("""
                SunnyBeam Solar Data Aggregator

                Usage: java -jar SunnyBeamAggregator.jar [OPTIONS]

                Options:
                  --data-dir=<path>   Path to the data directory (default: ./data)
                  --force             Force regeneration of all monthly files
                  --verbose           Enable verbose logging
                  --help, -h          Display this help message

                Examples:
                  java -jar SunnyBeamAggregator.jar
                  java -jar SunnyBeamAggregator.jar --data-dir=/path/to/data
                  java -jar SunnyBeamAggregator.jar --force --verbose

                The application expects CSV files in one of these formats:
                  - data/YY-MM-DD.csv (e.g., data/23-11-01.csv)
                  - data/YY-MM/YY-MM-DD.csv (e.g., data/23-11/23-11-01.csv)
            """.trimIndent())
        }
    }
}

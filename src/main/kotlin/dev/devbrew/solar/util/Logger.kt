package dev.devbrew.solar.util

/**
 * Simple logger for console output.
 */
class Logger(private val verbose: Boolean = false) {

    fun info(message: String) {
        println(message)
    }

    fun debug(message: String) {
        if (verbose) {
            println("[DEBUG] $message")
        }
    }

    fun warn(message: String) {
        println("[WARN] $message")
    }

    fun error(message: String) {
        System.err.println("[ERROR] $message")
    }
}

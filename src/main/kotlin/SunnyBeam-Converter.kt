import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

fun main() {
    println("Please enter the absolute path to the folder containing the CSV files:")
    val inputFolderPath = readLine() ?: return println("No input provided.")

    val folder = File("$inputFolderPath/")
    if (!folder.exists() || !folder.isDirectory) {
        return println("The provided path does not exist or is not a directory.")
    }

    val yearMonth = folder.name.replace('_', '-') // Assuming folder name format is "23_11"
    val outputFilePath = "${folder.parentFile.absolutePath}/${folder.name}.csv" // Output file in the same directory as the input folder

    println("Starting to generate monthly overview for $yearMonth into $outputFilePath")
    generateMonthlyOverview(inputFolderPath, outputFilePath, yearMonth)
}

fun generateMonthlyOverview(inputFolderPath: String, outputFilePath: String, yearMonth: String) {
    val startDate = LocalDate.parse("${yearMonth}-01", DateTimeFormatter.ofPattern("yy-MM-dd"))
    val daysInMonth = startDate.lengthOfMonth()

    val dateFormatter = DateTimeFormatterBuilder()
        .appendPattern("dd.MM.yyyy HH:mm")
        .toFormatter(Locale.GERMAN)

    val fileDateFormatter = DateTimeFormatter.ofPattern("yy-MM-dd")

    val headers = (1..daysInMonth).joinToString(separator = "\t", prefix = "Uhrzeit\t") { day ->
        startDate.plusDays((day - 1).toLong()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    val dataMap = (0 until 144).associate { index ->
        val hour = index / 6
        val minute = (index % 6) * 10
        LocalTime.of(hour, minute) to Array<String?>(daysInMonth) { null }
    }.toMutableMap()

    File(inputFolderPath).listFiles { _, name -> name.endsWith(".csv") || name.endsWith(".CSV") }?.also { files ->
        println("Found ${files.size} CSV files to process.")
    }?.forEach { file ->
        println("Processing file: ${file.name}")
        val fileDate = LocalDate.parse(file.nameWithoutExtension, fileDateFormatter)
        val dayIndex = fileDate.dayOfMonth - 1

        file.readLines().dropWhile { it.startsWith(";") }.also { lines ->
            println("Processing ${lines.size} lines from ${file.name}")
        }.forEachIndexed { index, line ->
            val parts = line.split(";")
            if (parts.size > 1 && parts[0].isNotBlank()) {
                try {
                    val dateTime = LocalDateTime.parse(parts[0], dateFormatter)
                    val time = dateTime.toLocalTime()
                    val value = parts[1].replace(",", ".").ifEmpty { null }
                    dataMap[time]?.set(dayIndex, value)
                } catch (e: Exception) {
                    println("Error parsing line $index in file ${file.name}: $line")
                    println("Exception: ${e.message}")
                }
            }
        }
    }

    File(outputFilePath).bufferedWriter().use { writer ->
        writer.write(headers + "\n")
        dataMap.toSortedMap().forEach { (time, values) ->
            val timeString = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            val row = values.joinToString(separator = "\t", prefix = "$timeString\t") { it ?: "" }
            writer.write(row + "\n")
        }
    }
    println("Monthly overview generated at $outputFilePath")
}

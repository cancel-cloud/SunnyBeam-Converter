plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "dev.devbrew.solar"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

    // Kotlinx serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
}

application {
    mainClass.set("dev.devbrew.solar.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

// Configure Shadow JAR
tasks.shadowJar {
    archiveBaseName.set("SunnyBeamAggregator")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")

    manifest {
        attributes["Main-Class"] = "dev.devbrew.solar.MainKt"
    }

    // Minimize JAR by removing unused classes
    minimize {
        exclude(dependency("org.jetbrains.kotlinx:.*"))
    }
}

// Make the build task depend on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

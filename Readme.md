# SunnyBeam CSV Converter

## Overview
The SunnyBeam CSV Converter is a simple yet powerful tool designed to aggregate daily CSV files from your SunnyBeam Solar Power Unit into a single monthly file. This utility streamlines data management, allowing for easier analysis and reporting of your solar power generation data.

## Features
- **Simplicity**: Easy to use with minimal setup.
- **Automation**: Automatically appends the necessary path separator if omitted.
- **Efficiency**: Aggregates all daily CSV files within a specified folder into a single comprehensive monthly file.

## Prerequisites
Before you begin, ensure you have Java Runtime Environment (JRE) installed on your computer to run the `.jar` file.

## Installation
1. Download the latest `SunnyBeamCSVConverter.jar` from the Releases section.
2. Place the JAR file in a suitable location on your computer.

## Usage
To use the SunnyBeam CSV Converter, you need the exact folder path where all the daily CSV files are stored.

### Running the Converter
Simply double-click the jar file, or right-click it and select "Run Jar" or by 
opening a terminal or command prompt and navigate to the folder containing `SunnyBeamCSVConverter.jar`. Execute the following command:

```bash
java -jar SunnyBeamCSVConverter.jar /path/to/csv/folder

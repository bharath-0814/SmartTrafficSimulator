package com.traffic.service;

import com.traffic.engine.SimulationEngine;
import com.traffic.engine.SimulationSnapshot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Service responsible for generating, writing, and reading simulation audit reports.
 *
 * OOP Concepts & Syllabus Topics Demonstrated:
 * 1. File I/O with Character Streams (Lectures 33 & 34):
 *    - Uses FileWriter to write text reports to disk.
 *    - Uses FileReader wrapped in BufferedReader for efficient line-by-line reading.
 * 2. Exception Handling (Lectures 36, 37 & 38):
 *    - Demonstrates explicit try, catch, and finally blocks.
 *    - Performs guaranteed resource cleanup in the finally block.
 *    - Wraps low-level IOExceptions into a meaningful checked custom ReportException.
 * 3. Decoupling & Thread Safety:
 *    - Keeps file-handling completely outside the SimulationEngine and domain models.
 *    - Consumes immutable {@link SimulationSnapshot} objects to ensure thread-safe reporting.
 */
public class ReportService {

    public static final String DEFAULT_REPORT_FILE = "traffic_report.txt";

    /**
     * Generates and writes the simulation report to the default file ("traffic_report.txt").
     *
     * @param engine The simulation engine containing execution metrics.
     * @throws ReportException If file creation or writing fails.
     */
    public void generateReport(SimulationEngine engine) throws ReportException {
        generateReport(engine, DEFAULT_REPORT_FILE);
    }

    /**
     * Formats and writes simulation statistics from an engine to the specified file path.
     * Extracts an atomic {@link SimulationSnapshot} to prevent concurrent mutation issues.
     *
     * @param engine   The simulation engine containing current metrics.
     * @param filePath The file destination path (e.g., "traffic_report.txt").
     * @throws ReportException If an I/O error occurs while writing the report.
     */
    public void generateReport(SimulationEngine engine, String filePath) throws ReportException {
        if (engine == null) {
            throw new IllegalArgumentException("SimulationEngine cannot be null.");
        }
        generateReport(engine.getSnapshot(), filePath);
    }

    /**
     * Formats and writes simulation statistics from an immutable snapshot to the specified file path.
     * Demonstrates try, catch, and finally blocks for guaranteed resource cleanup.
     *
     * @param snapshot The immutable simulation state snapshot.
     * @param filePath The file destination path.
     * @throws ReportException If an I/O error occurs while writing the report.
     */
    public void generateReport(SimulationSnapshot snapshot, String filePath) throws ReportException {
        if (snapshot == null) {
            throw new IllegalArgumentException("SimulationSnapshot cannot be null.");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        FileWriter writer = null;
        try {
            // Step 1: Open FileWriter character stream
            writer = new FileWriter(filePath);

            // Step 2: Format the simulation report contents
            String reportText = formatReport(snapshot);

            // Step 3: Write report to file
            writer.write(reportText);
            writer.flush();

        } catch (IOException e) {
            // Wrap low-level IOException inside our custom checked ReportException
            throw new ReportException("Failed to write simulation report to: " + filePath, e);

        } finally {
            // Step 4: Guaranteed resource cleanup inside finally block
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to cleanly close FileWriter: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reads the entire contents of the default report file ("traffic_report.txt").
     *
     * @return The report text read from disk.
     * @throws ReportException If the file cannot be found or read.
     */
    public String readReport() throws ReportException {
        return readReport(DEFAULT_REPORT_FILE);
    }

    /**
     * Reads a report file line-by-line using FileReader and BufferedReader.
     * Demonstrates try, catch, and finally blocks.
     *
     * @param filePath The path of the report file to read.
     * @return The full text content of the report file.
     * @throws ReportException If reading fails or the file does not exist.
     */
    public String readReport(String filePath) throws ReportException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder reportBuilder = new StringBuilder();

        try {
            fileReader = new FileReader(filePath);
            bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                reportBuilder.append(line).append(System.lineSeparator());
            }

            return reportBuilder.toString();

        } catch (IOException e) {
            // Wrap checked IOException inside our checked domain exception
            throw new ReportException("Failed to read simulation report from: " + filePath, e);

        } finally {
            // Always close streams in finally block in reverse order of opening
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close BufferedReader: " + e.getMessage());
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close FileReader: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reads and displays a report to standard output.
     *
     * @param filePath Path to the report file.
     * @throws ReportException If the report file cannot be read.
     */
    public void displayReport(String filePath) throws ReportException {
        String content = readReport(filePath);
        System.out.print(content);
    }

    /**
     * Reads and displays the default report ("traffic_report.txt").
     */
    public void displayReport() throws ReportException {
        displayReport(DEFAULT_REPORT_FILE);
    }

    /**
     * Formats simulation statistics into a clean, human-readable text document from an engine.
     *
     * @param engine The simulation engine to extract metrics from.
     * @return Formatted multi-line string report.
     */
    public String formatReport(SimulationEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("SimulationEngine cannot be null.");
        }
        return formatReport(engine.getSnapshot());
    }

    /**
     * Formats simulation statistics from an immutable snapshot into a clean, human-readable text document.
     *
     * @param snapshot The immutable simulation state snapshot.
     * @return Formatted multi-line string report.
     */
    public String formatReport(SimulationSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("SimulationSnapshot cannot be null.");
        }
        int totalServed = snapshot.getTotalVehiclesServed();
        int emergencyServed = snapshot.getTotalEmergencyVehiclesServed();
        int standardServed = totalServed - emergencyServed;

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("          SMART TRAFFIC SIMULATOR — SIMULATION AUDIT REPORT\n");
        sb.append("================================================================================\n");
        sb.append("Generated By        : ReportService (com.traffic.service)\n");
        sb.append("Course              : CSC2104 - Object Oriented Programming\n");
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("1. SIMULATION EXECUTION METRICS\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Simulation Duration     : %d seconds (ticks)\n", snapshot.getTotalTicks()));
        sb.append(String.format("Configured Green Phase  : %d seconds\n", snapshot.getGreenDuration()));
        sb.append(String.format("Configured Yellow Phase : %d seconds\n", snapshot.getYellowDuration()));
        sb.append(String.format("Starvation Threshold    : %d seconds (default is 20s)\n", snapshot.getStarvationThreshold()));
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("2. TRAFFIC FLOW & VEHICLES SERVED\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Vehicles Served          : %d\n", totalServed));
        sb.append(String.format("Emergency Vehicles Served      : %d\n", emergencyServed));
        sb.append(String.format("Standard Commuter Vehicles     : %d\n", standardServed));
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("3. REMAINING QUEUES BY APPROACH\n");
        sb.append("--------------------------------------------------------------------------------\n");

        int totalWaiting = 0;
        List<SimulationSnapshot.RoadSnapshot> roads = snapshot.getRoads();
        for (SimulationSnapshot.RoadSnapshot road : roads) {
            int queueLen = road.getQueueLength();
            totalWaiting += queueLen;

            String ids = formatQueueSnapshotIds(road);
            sb.append(String.format("- %-15s (%-5s) : %2d waiting | %s\n",
                    road.getName(), road.getDirection(), queueLen, ids));
        }
        sb.append(String.format("Total Vehicles Still Waiting   : %d\n", totalWaiting));
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("4. INTERSECTION STATE AT COMPLETION\n");
        sb.append("--------------------------------------------------------------------------------\n");
        SimulationSnapshot.RoadSnapshot activeRoad = snapshot.getActiveRoad();
        sb.append(String.format("Active Approach         : %s\n", activeRoad != null ? activeRoad.getName() : "None"));
        sb.append(String.format("Active Light Color      : %s\n", activeRoad != null ? activeRoad.getSignalColor() : "RED"));
        sb.append(String.format("Emergency Override Mode : %s\n", (snapshot.isEmergencyActive() ? "ACTIVE" : "INACTIVE")));
        sb.append(String.format("Last Engine Event       : %s\n", snapshot.getLastEventDescription()));
        sb.append("================================================================================\n");

        return sb.toString();
    }

    private String formatQueueSnapshotIds(SimulationSnapshot.RoadSnapshot road) {
        List<SimulationSnapshot.VehicleSnapshot> vehicles = road.getVehicles();
        if (vehicles.isEmpty()) {
            return "Queue: (empty)";
        }
        StringBuilder sb = new StringBuilder("Queue: [");
        for (int i = 0; i < vehicles.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vehicles.get(i).getId());
        }
        sb.append("]");
        return sb.toString();
    }
}

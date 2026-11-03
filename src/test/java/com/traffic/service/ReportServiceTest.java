package com.traffic.service;

import com.traffic.engine.SimulationEngine;
import com.traffic.model.Ambulance;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for ReportService and ReportException (Phase 4).
 *
 * Verifies:
 * 1. Successful report file generation via FileWriter.
 * 2. Proper presence of all required simulation metrics and statistics.
 * 3. Successful reading of report files via FileReader and BufferedReader.
 * 4. Error handling and throwing of custom checked ReportException for inaccessible files.
 * 5. Input validation (null/empty arguments).
 * 6. ReportException constructor and cause-wrapping mechanisms.
 */
class ReportServiceTest {

    private static final String TEST_REPORT_PATH = "target/test_traffic_report.txt";

    private ReportService reportService;
    private Intersection intersection;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        intersection = new Intersection();
        // Green=3s, Yellow=2s, StarvationThreshold=10s
        engine = new SimulationEngine(intersection, 3, 2, 10);
    }

    @AfterEach
    void tearDown() {
        File file = new File(TEST_REPORT_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Test 1: Successful report generation and file persistence")
    void testSuccessfulReportGeneration() throws ReportException {
        // Run a couple of ticks
        engine.tick();
        engine.tick();

        reportService.generateReport(engine, TEST_REPORT_PATH);

        File file = new File(TEST_REPORT_PATH);
        assertTrue(file.exists(), "Report file should exist on disk");
        assertTrue(file.isFile(), "Report path should point to a regular file");
        assertTrue(file.length() > 0, "Report file should not be empty");
    }

    @Test
    @DisplayName("Test 2: Report contains expected statistics and approach metrics")
    void testReportContainsExpectedStatistics() throws ReportException {
        // Step 1: Add Car to North
        Car car = new Car("CAR-N1", 40, 2);
        intersection.getRoad(Direction.NORTH).addVehicle(car);

        // Tick 1: North is GREEN -> CAR-N1 departs
        engine.tick();
        assertEquals(1, engine.getTotalVehiclesServed());

        // Step 2: Add Ambulance to South while North is actively GREEN
        Ambulance ambulance = new Ambulance("AMB-S1", 80, 5, "Trauma Center");
        intersection.getRoad(Direction.SOUTH).addVehicle(ambulance);

        // Tick 2: North curtails to YELLOW due to AMB-S1 preemption
        engine.tick();
        // Tick 3: North is in YELLOW (timer 2 -> 1)
        engine.tick();
        // Tick 4: North turns RED, South gets GREEN
        engine.tick();
        // Tick 5: South is GREEN -> AMB-S1 departs!
        engine.tick();

        assertEquals(2, engine.getTotalVehiclesServed());
        assertEquals(1, engine.getTotalEmergencyVehiclesServed());

        // Generate report
        reportService.generateReport(engine, TEST_REPORT_PATH);
        String reportText = reportService.readReport(TEST_REPORT_PATH);

        // Verify key sections and statistics in report
        assertTrue(reportText.contains("SMART TRAFFIC SIMULATOR — SIMULATION AUDIT REPORT"));
        assertTrue(reportText.contains("Simulation Duration"));
        assertTrue(reportText.contains("Total Vehicles Served          : 2"));
        assertTrue(reportText.contains("Emergency Vehicles Served      : 1"));
        assertTrue(reportText.contains("Standard Commuter Vehicles     : 1"));
        assertTrue(reportText.contains("REMAINING QUEUES BY APPROACH"));
        assertTrue(reportText.contains("North Approach"));
        assertTrue(reportText.contains("East Approach"));
        assertTrue(reportText.contains("South Approach"));
        assertTrue(reportText.contains("West Approach"));
        assertTrue(reportText.contains("INTERSECTION STATE AT COMPLETION"));
    }

    @Test
    @DisplayName("Test 3: Reading report via BufferedReader matches generated content")
    void testReadReportMatchesGeneratedContent() throws ReportException {
        engine.step(3);

        String expectedContent = reportService.formatReport(engine);
        reportService.generateReport(engine, TEST_REPORT_PATH);

        String actualContent = reportService.readReport(TEST_REPORT_PATH);

        assertNotNull(actualContent);
        // Normalize line separators for cross-platform comparison
        assertEquals(expectedContent.replace("\r\n", "\n").trim(),
                actualContent.replace("\r\n", "\n").trim());
    }

    @Test
    @DisplayName("Test 4: Reading a non-existent file throws custom ReportException")
    void testReadingNonExistentFileThrowsReportException() {
        String invalidPath = "target/non_existent_dir_999/does_not_exist.txt";

        ReportException ex = assertThrows(ReportException.class, () ->
                reportService.readReport(invalidPath));

        assertTrue(ex.getMessage().contains("Failed to read simulation report"));
        assertNotNull(ex.getCause(), "Root cause (IOException / FileNotFoundException) must be preserved");
    }

    @Test
    @DisplayName("Test 5: Writing to an inaccessible path throws custom ReportException")
    void testWritingToInaccessiblePathThrowsReportException(@TempDir Path tempDir) throws IOException {
        // Create a regular file in the temporary directory
        File blockerFile = tempDir.resolve("blocker_file.txt").toFile();
        assertTrue(blockerFile.createNewFile(), "Blocker file should be created");

        // Attempting to write a file where a parent path component is a regular file
        // (rather than a directory) causes FileWriter / parentDir.mkdirs to fail portably across OSes
        String invalidPath = blockerFile.getAbsolutePath() + File.separator + "traffic_report.txt";

        ReportException ex = assertThrows(ReportException.class, () ->
                reportService.generateReport(engine, invalidPath));

        assertTrue(ex.getMessage().contains("Failed to write simulation report"));
        assertNotNull(ex.getCause(), "Root cause (IOException) must be wrapped");
    }

    @Test
    @DisplayName("Test 6: Parameter validation throws IllegalArgumentException for null/empty arguments")
    void testParameterValidation() {
        assertThrows(IllegalArgumentException.class, () -> reportService.generateReport(null, TEST_REPORT_PATH));
        assertThrows(IllegalArgumentException.class, () -> reportService.generateReport(engine, null));
        assertThrows(IllegalArgumentException.class, () -> reportService.generateReport(engine, "   "));
        assertThrows(IllegalArgumentException.class, () -> reportService.readReport(null));
        assertThrows(IllegalArgumentException.class, () -> reportService.readReport(""));
        assertThrows(IllegalArgumentException.class, () -> reportService.readReport("   "));
    }

    @Test
    @DisplayName("Test 7: Custom ReportException behaves properly with message and cause")
    void testReportExceptionConstructors() {
        ReportException ex1 = new ReportException("Report error occurred.");
        assertEquals("Report error occurred.", ex1.getMessage());

        IOException cause = new IOException("Disk full");
        ReportException ex2 = new ReportException("I/O failure", cause);
        assertEquals("I/O failure", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
}

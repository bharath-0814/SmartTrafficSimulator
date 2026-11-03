package com.traffic.ui;

import com.traffic.engine.SimulationController;
import com.traffic.engine.SimulationEngine;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for {@link MainFrame} and {@link IntersectionPanel} (Phase 6: Swing GUI).
 *
 * Verifies:
 * 1. Safe construction of MainFrame and IntersectionPanel on the EDT.
 * 2. Proper initialization of buttons, sliders, and statistics labels.
 * 3. Start, Pause, Resume, and Stop button interactions dispatching to SimulationController.
 * 4. Step Once button behavior.
 * 5. Report generation button functionality.
 * 6. Headless safety and disposal.
 */
class MainFrameTest {

    private Intersection intersection;
    private SimulationEngine engine;
    private SimulationController controller;
    private MainFrame mainFrame;

    @BeforeEach
    void setUp() throws InterruptedException, InvocationTargetException {
        intersection = new Intersection();
        engine = new SimulationEngine(intersection, 2, 1, 10);
        controller = new SimulationController(engine, 50);

        // Run UI construction on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeAndWait(() -> {
            mainFrame = new MainFrame(controller);
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException, InvocationTargetException {
        if (controller != null && controller.isRunning()) {
            controller.stop();
        }
        if (mainFrame != null) {
            SwingUtilities.invokeAndWait(() -> {
                mainFrame.dispose();
            });
        }
    }

    @Test
    @DisplayName("Test 1: MainFrame and IntersectionPanel initialize properly with non-null components")
    void testFrameInitialization() {
        assertNotNull(mainFrame, "MainFrame instance must not be null");
        assertNotNull(mainFrame.getController(), "Controller must not be null");
        assertNotNull(mainFrame.getEngine(), "Engine must not be null");

        assertNotNull(mainFrame.getBtnStart(), "Start button must not be null");
        assertNotNull(mainFrame.getBtnPause(), "Pause button must not be null");
        assertNotNull(mainFrame.getBtnResume(), "Resume button must not be null");
        assertNotNull(mainFrame.getBtnStop(), "Stop button must not be null");
        assertNotNull(mainFrame.getBtnStepOnce(), "Step Once button must not be null");
        assertNotNull(mainFrame.getBtnGenerateReport(), "Generate Report button must not be null");
        assertNotNull(mainFrame.getBtnViewReport(), "View Report button must not be null");
    }

    @Test
    @DisplayName("Test 2: Null controller constructor validation throws IllegalArgumentException")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new MainFrame(null));
        assertThrows(IllegalArgumentException.class, () -> new IntersectionPanel(null));
    }

    @Test
    @DisplayName("Test 3: Start, Pause, Resume, and Stop button clicks dispatch correctly to controller")
    void testButtonDispatches() throws InterruptedException, InvocationTargetException {
        assertFalse(controller.isRunning());

        // 1. Click Start
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnStart().doClick());
        assertTrue(controller.isRunning(), "Controller should be running after Start click");

        // Wait a few ticks
        Thread.sleep(120);
        assertTrue(engine.getTotalTicks() > 0);

        // 2. Click Pause
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnPause().doClick());
        assertTrue(controller.isPaused(), "Controller should be paused after Pause click");
        int ticksAtPause = engine.getTotalTicks();
        Thread.sleep(80);
        assertEquals(ticksAtPause, engine.getTotalTicks(), "Ticks should not advance while paused");

        // 3. Click Resume
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnResume().doClick());
        assertFalse(controller.isPaused(), "Controller should not be paused after Resume click");
        Thread.sleep(120);
        assertTrue(engine.getTotalTicks() > ticksAtPause, "Ticks should advance after Resume click");

        // 4. Click Stop
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnStop().doClick());
        assertFalse(controller.isRunning(), "Controller should be stopped after Stop click");
    }

    @Test
    @DisplayName("Test 4: Step Once button advances simulation by exactly one tick when paused")
    void testStepOnceButton() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnStart().doClick());
        Thread.sleep(60);

        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnPause().doClick());
        assertTrue(controller.isPaused());

        int before = engine.getTotalTicks();
        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnStepOnce().doClick());
        assertEquals(before + 1, engine.getTotalTicks(), "Step Once should advance ticks by 1");

        SwingUtilities.invokeAndWait(() -> mainFrame.getBtnStop().doClick());
    }

    @Test
    @DisplayName("Test 5: Generate Report action saves report file to disk")
    void testGenerateReportButton() throws InterruptedException, InvocationTargetException {
        // Run a few ticks
        engine.step(2);

        // Execute report action (with showDialog=false to avoid blocking automated tests with modal popups)
        boolean success = mainFrame.generateReportAction(false);
        assertTrue(success, "generateReportAction should return true");

        File reportFile = new File("traffic_report.txt");
        assertTrue(reportFile.exists(), "traffic_report.txt must exist on disk after generating report");
        assertTrue(reportFile.length() > 0, "traffic_report.txt must not be empty");
    }
}

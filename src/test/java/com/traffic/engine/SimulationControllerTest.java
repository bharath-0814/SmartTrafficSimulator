package com.traffic.engine;

import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import com.traffic.model.Road;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for {@link SimulationController} (Phase 5: Multithreading).
 *
 * Verifies:
 * 1. Background worker thread starts and enters RUNNABLE/TIMED_WAITING state.
 * 2. Simulation ticks advance automatically while running.
 * 3. Pausing suspends tick progression without killing the thread.
 * 4. Resuming restarts tick progression.
 * 5. Stopping terminates the worker thread safely without deprecated methods.
 * 6. Starting after stopping properly spawns a new worker thread.
 * 7. Manual stepping via stepOnce() functions while paused.
 * 8. Constructor and setter parameter validation.
 */
class SimulationControllerTest {

    private Intersection intersection;
    private SimulationEngine engine;
    private SimulationController controller;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
        // Green=2s, Yellow=1s, StarvationThreshold=10s
        engine = new SimulationEngine(intersection, 2, 1, 10);
        // Fast tick delay (25ms) for rapid and deterministic unit tests
        controller = new SimulationController(engine, 25);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (controller != null && controller.isRunning()) {
            controller.stop();
            controller.join(1000);
        }
    }

    @Test
    @DisplayName("Test 1: Initial controller state before starting")
    void testInitialState() {
        assertFalse(controller.isRunning(), "Controller should not be running initially");
        assertFalse(controller.isPaused(), "Controller should not be paused initially");
        assertNull(controller.getWorkerThread(), "Worker thread should be null before start()");
        assertEquals(engine, controller.getEngine(), "Engine getter should return configured engine");
        assertEquals(25, controller.getTickDelayMs(), "Tick delay should match configured value");
    }

    @Test
    @DisplayName("Test 2: Background thread starts and advances simulation ticks")
    void testThreadStartsAndAdvancesTicks() throws InterruptedException {
        controller.start();

        assertTrue(controller.isRunning(), "Controller should be running after start()");
        Thread worker = controller.getWorkerThread();
        assertNotNull(worker, "Worker thread must not be null");
        assertTrue(worker.isAlive(), "Worker thread must be alive");
        assertEquals("TrafficSimulation-WorkerThread", worker.getName());

        // Wait ~120ms (with 25ms delay, at least 2 ticks should occur)
        Thread.sleep(120);

        int ticks = engine.getTotalTicks();
        assertTrue(ticks >= 2, "Engine should have executed multiple ticks in background thread (was: " + ticks + ")");
    }

    @Test
    @DisplayName("Test 3: Pause halts tick progression while keeping thread alive")
    void testPauseHaltsTickProgression() throws InterruptedException {
        controller.start();

        // Let simulation run for ~80ms to accumulate some ticks
        Thread.sleep(80);
        assertTrue(engine.getTotalTicks() > 0, "Ticks should have advanced");

        // Pause simulation
        controller.pause();
        assertTrue(controller.isPaused(), "Controller should report paused state");

        // Record ticks and wait ~100ms
        int ticksAtPause = engine.getTotalTicks();
        Thread.sleep(100);

        // Ticks should remain unchanged while paused
        assertEquals(ticksAtPause, engine.getTotalTicks(),
                "Simulation ticks must NOT advance while paused");
        assertTrue(controller.isRunning(),
                "Worker thread should remain alive while paused, waiting to be resumed");
    }

    @Test
    @DisplayName("Test 4: Resume restarts tick progression after pause")
    void testResumeRestartsTickProgression() throws InterruptedException {
        controller.start();
        Thread.sleep(60);

        controller.pause();
        Thread.sleep(60);
        int ticksAtPause = engine.getTotalTicks();

        // Resume simulation
        controller.resume();
        assertFalse(controller.isPaused(), "Controller should no longer be paused");

        // Wait ~100ms for ticks to resume
        Thread.sleep(100);
        int ticksAfterResume = engine.getTotalTicks();

        assertTrue(ticksAfterResume > ticksAtPause,
                "Simulation ticks should have increased after resuming (was: " + ticksAtPause + ", now: " + ticksAfterResume + ")");
    }

    @Test
    @DisplayName("Test 5: Stop terminates worker thread cleanly and safely")
    void testStopTerminatesThreadSafely() throws InterruptedException {
        controller.start();
        Thread.sleep(60);

        Thread worker = controller.getWorkerThread();
        assertTrue(worker.isAlive());

        // Stop the simulation
        controller.stop();
        controller.join(1000);

        assertFalse(controller.isRunning(), "Controller should not be running after stop()");
        assertFalse(worker.isAlive(), "Worker thread should be dead after stop() and join()");
        assertEquals(Thread.State.TERMINATED, worker.getState(), "Worker thread must be TERMINATED");

        // Ensure ticks no longer increase
        int ticksAtStop = engine.getTotalTicks();
        Thread.sleep(60);
        assertEquals(ticksAtStop, engine.getTotalTicks(), "Ticks must not advance after stopping");
    }

    @Test
    @DisplayName("Test 6: Starting after stopping cleanly creates and starts a new thread")
    void testRestartAfterStop() throws InterruptedException {
        controller.start();
        Thread.sleep(50);
        Thread worker1 = controller.getWorkerThread();

        controller.stop();
        controller.join(1000);
        assertFalse(worker1.isAlive());

        // Restart simulation
        controller.start();
        Thread worker2 = controller.getWorkerThread();

        assertTrue(controller.isRunning());
        assertTrue(worker2.isAlive());
        assertFalse(worker1 == worker2, "New worker thread must be created upon restarting");

        Thread.sleep(60);
        assertTrue(engine.getTotalTicks() > 0);
    }

    @Test
    @DisplayName("Test 7: Manual stepOnce() advances simulation by 1 tick while paused")
    void testStepOnceWhilePaused() throws InterruptedException {
        controller.start();
        Thread.sleep(50);
        controller.pause();
        Thread.sleep(50);

        int ticksBefore = engine.getTotalTicks();
        controller.stepOnce();
        assertEquals(ticksBefore + 1, engine.getTotalTicks(), "stepOnce() must advance engine by exactly 1 tick");

        controller.stepOnce();
        assertEquals(ticksBefore + 2, engine.getTotalTicks(), "Second stepOnce() must advance engine by another 1 tick");
    }

    @Test
    @DisplayName("Test 8: Parameter validation throws IllegalArgumentException for invalid inputs")
    void testParameterValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(null, 100));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(engine, 0));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(engine, -50));
        assertThrows(IllegalArgumentException.class, () -> controller.setTickDelayMs(0));
        assertThrows(IllegalArgumentException.class, () -> controller.setTickDelayMs(-10));
    }

    @Test
    @DisplayName("Test 9: Dynamic speed adjustment via setTickDelayMs() works cleanly")
    void testSetTickDelayMs() {
        controller.setTickDelayMs(200);
        assertEquals(200, controller.getTickDelayMs());

        controller.setTickDelayMs(500);
        assertEquals(500, controller.getTickDelayMs());
    }

    @Test
    @DisplayName("Test 10: stepOnce() throws IllegalStateException when simulation is not paused")
    void testStepOnceFailsWhenNotPaused() throws InterruptedException {
        // 1. Calling stepOnce() before simulation has started and paused
        assertFalse(controller.isPaused());
        assertThrows(IllegalStateException.class, () -> controller.stepOnce(),
                "stepOnce() must throw IllegalStateException when called in unpaused state");

        // 2. Calling stepOnce() while actively running
        controller.start();
        assertThrows(IllegalStateException.class, () -> controller.stepOnce(),
                "stepOnce() must throw IllegalStateException while running unpaused");

        // 3. Once paused, stepOnce() must succeed
        controller.pause();
        assertTrue(controller.isPaused());
        int ticksBefore = engine.getTotalTicks();
        controller.stepOnce();
        assertEquals(ticksBefore + 1, engine.getTotalTicks(), "stepOnce() should succeed when paused");
    }

    @Test
    @DisplayName("Test 11: pause() race condition guarantee: no additional tick begins once pause() returns")
    void testPauseGuaranteesNoTickAfterReturn() throws InterruptedException {
        controller.start();
        Thread.sleep(60);

        // Call pause - must block until any in-progress tick finishes and guarantee no subsequent tick starts
        controller.pause();
        int ticksAtPauseReturn = engine.getTotalTicks();

        // Wait to verify that no delayed tick started or completed after pause() returned
        Thread.sleep(100);
        assertEquals(ticksAtPauseReturn, engine.getTotalTicks(),
                "No tick should occur after pause() returns");
    }
}

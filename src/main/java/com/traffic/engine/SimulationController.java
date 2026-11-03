package com.traffic.engine;

/**
 * SimulationController manages the execution of {@link SimulationEngine}
 * on a dedicated background {@link Thread} by implementing {@link Runnable}.
 *
 * <p>OOP & Java Concepts Demonstrated:
 * <ul>
 *   <li><b>Multithreading via Runnable:</b> Decouples thread execution logic from
 *       class inheritance hierarchy (CSC2104 Lectures 40-41).</li>
 *   <li><b>Thread Lifecycle:</b> Demonstrates thread creation, execution, timed waiting
 *       via {@link Thread#sleep(long)}, and safe termination (Lectures 42-43).</li>
 *   <li><b>Synchronization & Inter-Thread Communication:</b> Uses {@code synchronized}
 *       blocks and {@code wait()} / {@code notifyAll()} on a dedicated monitor lock to
 *       pause and resume tick progression without busy-waiting (Lectures 45-46).</li>
 *   <li><b>Volatile State Flags:</b> Uses {@code volatile} on boolean state flags to
 *       ensure instant memory visibility across thread caches (Lecture 47).</li>
 *   <li><b>Safe Thread Termination:</b> Avoids deprecated {@code Thread.stop()} by
 *       checking running flags and handling {@link InterruptedException} cleanly (Lecture 47).</li>
 * </ul>
 */
public class SimulationController implements Runnable {

    /** Default delay between simulation ticks in milliseconds (1 second). */
    public static final int DEFAULT_TICK_DELAY_MS = 1000;

    /** The underlying traffic simulation engine. */
    private final SimulationEngine engine;

    /** Configured delay between simulation ticks in milliseconds. */
    private volatile int tickDelayMs;

    /** The dedicated background worker thread. */
    private Thread workerThread;

    /**
     * Volatile flag indicating whether the simulation loop is active.
     * Reading and writing this flag is guaranteed to be visible to all threads.
     */
    private volatile boolean running;

    /**
     * Volatile flag indicating whether tick progression is currently suspended.
     */
    private volatile boolean paused;

    /**
     * Dedicated monitor object used for inter-thread wait/notify coordination
     * during pause and resume operations.
     */
    private final Object pauseLock = new Object();

    /**
     * Constructs a SimulationController with a specified engine and custom tick delay.
     *
     * @param engine      The traffic simulation engine to run.
     * @param tickDelayMs Interval between ticks in milliseconds (must be positive).
     * @throws IllegalArgumentException if engine is null or tickDelayMs is non-positive.
     */
    public SimulationController(SimulationEngine engine, int tickDelayMs) {
        if (engine == null) {
            throw new IllegalArgumentException("Simulation engine cannot be null.");
        }
        if (tickDelayMs <= 0) {
            throw new IllegalArgumentException("Tick delay must be positive: " + tickDelayMs);
        }
        this.engine = engine;
        this.tickDelayMs = tickDelayMs;
        this.running = false;
        this.paused = false;
    }

    /**
     * Overloaded constructor using the default tick interval of 1000ms (1 second).
     *
     * @param engine The traffic simulation engine to run.
     */
    public SimulationController(SimulationEngine engine) {
        this(engine, DEFAULT_TICK_DELAY_MS);
    }

    // =========================================================================
    // Thread Lifecycle Controls: Start, Pause, Resume, Stop
    // =========================================================================

    /**
     * Starts the simulation in a new background thread.
     * If already running and paused, calling start() resumes tick progression.
     * If already actively running, this call is safely ignored.
     */
    public synchronized void start() {
        if (running && paused) {
            resume();
            return;
        }
        if (running && workerThread != null && workerThread.isAlive()) {
            return; // Already actively running
        }

        running = true;
        paused = false;
        workerThread = new Thread(this, "TrafficSimulation-WorkerThread");
        workerThread.start();
    }

    /**
     * Pauses the simulation. Ensures that once this method returns,
     * no additional simulation tick can begin.
     */
    public void pause() {
        synchronized (pauseLock) {
            paused = true;
        }
    }

    /**
     * Resumes simulation execution if it is currently paused.
     * Notifies the background thread to exit the WAITING state.
     */
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            // Wakes up worker thread waiting on pauseLock inside run()
            pauseLock.notifyAll();
        }
    }

    /**
     * Safely terminates the background simulation thread.
     * Sets running flag to false, wakes up the worker thread if paused or sleeping,
     * and waits for the thread to terminate cleanly.
     */
    public synchronized void stop() {
        running = false;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll(); // Wake up thread if waiting in pauseLock
        }
        if (workerThread != null) {
            workerThread.interrupt(); // Interrupt Thread.sleep() to exit immediately
            try {
                workerThread.join(1000); // Wait for thread termination
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Manually advances the simulation by exactly one tick.
     * Only permitted when the simulation is actively paused.
     *
     * @throws IllegalStateException if the simulation is not paused.
     */
    public void stepOnce() {
        synchronized (pauseLock) {
            if (!paused) {
                throw new IllegalStateException("Simulation must be paused to perform a manual step.");
            }
            engine.tick();
        }
    }

    /**
     * Waits for the background simulation thread to finish execution.
     *
     * @throws InterruptedException if interrupted while waiting.
     */
    public void join() throws InterruptedException {
        if (workerThread != null) {
            workerThread.join();
        }
    }

    /**
     * Waits at most the given milliseconds for the background thread to finish.
     *
     * @param millis Maximum milliseconds to wait.
     * @throws InterruptedException if interrupted while waiting.
     */
    public void join(long millis) throws InterruptedException {
        if (workerThread != null) {
            workerThread.join(millis);
        }
    }

    // =========================================================================
    // Runnable Implementation
    // =========================================================================

    /**
     * The main execution loop executed by the background worker thread.
     * Continuously calls {@link SimulationEngine#tick()} and sleeps for
     * {@code tickDelayMs} until stopped.
     */
    @Override
    public void run() {
        while (running) {
            // -------------------------------------------------------------
            // Step 1: Handle Pause State & Execute Tick with Pause Safety
            // -------------------------------------------------------------
            synchronized (pauseLock) {
                while (paused && running) {
                    try {
                        // Releases pauseLock and enters WAITING state with zero CPU consumption
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        // Thread was interrupted while waiting; restore flag and exit cleanly
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Check if simulation was stopped while paused
                if (!running) {
                    break;
                }

                // Step 2: Advance Simulation by One Tick inside pauseLock
                // Ensuring that once pause() returns, no additional tick can begin
                engine.tick();
            }

            // -------------------------------------------------------------
            // Step 3: Sleep for the Configured Interval (Timed Waiting)
            // -------------------------------------------------------------
            try {
                Thread.sleep(tickDelayMs);
            } catch (InterruptedException e) {
                // Thread was interrupted during sleep (usually triggered by stop());
                // restore interrupted status and break out of loop cleanly
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    /**
     * Returns whether the background simulation thread is currently alive and active.
     *
     * @return true if the worker thread is alive and running, false otherwise.
     */
    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }

    /**
     * Returns whether tick progression is currently paused.
     *
     * @return true if paused, false otherwise.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Returns the underlying simulation engine.
     *
     * @return The SimulationEngine instance.
     */
    public SimulationEngine getEngine() {
        return engine;
    }

    /**
     * Returns the interval between simulation ticks in milliseconds.
     *
     * @return Tick delay in milliseconds.
     */
    public int getTickDelayMs() {
        return tickDelayMs;
    }

    /**
     * Adjusts the simulation speed by updating the tick delay interval.
     * Takes effect immediately on the next tick due to {@code volatile} visibility.
     *
     * @param tickDelayMs Interval in milliseconds (must be positive).
     */
    public void setTickDelayMs(int tickDelayMs) {
        if (tickDelayMs <= 0) {
            throw new IllegalArgumentException("Tick delay must be positive: " + tickDelayMs);
        }
        this.tickDelayMs = tickDelayMs;
    }

    /**
     * Returns the underlying background worker thread instance.
     *
     * @return The worker Thread, or null if start() has not been called.
     */
    public Thread getWorkerThread() {
        return workerThread;
    }
}

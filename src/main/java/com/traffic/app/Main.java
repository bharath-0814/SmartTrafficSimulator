package com.traffic.app;

import com.traffic.engine.SimulationController;
import com.traffic.engine.SimulationEngine;
import com.traffic.model.Ambulance;
import com.traffic.model.Bike;
import com.traffic.model.Bus;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import com.traffic.service.ReportException;
import com.traffic.service.ReportService;
import com.traffic.ui.MainFrame;

import java.io.File;

/**
 * Main entry point for the Smart Traffic Management and
 * Emergency Vehicle Prioritization Simulator.
 *
 * Course: CSC2104 - Object Oriented Programming (3rd Semester B.Tech CSE)
 * Manipal University Jaipur
 *
 * Demonstrates:
 * - Phase 1: Clean project initialization and environment setup.
 * - Phase 2: Domain OOP models, encapsulation, multilevel inheritance, polymorphism.
 * - Phase 3: Console simulation, round-robin rotation, safe emergency preemption.
 * - Phase 4: File I/O (FileWriter, FileReader, BufferedReader) and custom ReportException.
 */
public class Main {

    public static final String APP_NAME = "Smart Traffic Management & Emergency Vehicle Prioritization Simulator";
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("--gui") || args[0].equalsIgnoreCase("-g"))) {
            MainFrame.main(args);
            return;
        }
        printBanner();
        runConsoleSimulation();
    }

    private static void printBanner() {
        System.out.println("===================================================================================");
        System.out.println("  " + APP_NAME);
        System.out.println("  Version: " + VERSION);
        System.out.println("  Course: CSC2104 - Object Oriented Programming");
        System.out.println("===================================================================================");
    }

    /**
     * Executes the Phase 3 console demonstration.
     */
    public static void runConsoleSimulation() {
        System.out.println("\n>>> [ACT 1] INITIALIZING INTERSECTION AND VEHICLE QUEUES <<<");

        Intersection intersection = new Intersection();

        // 1. Populate the 4 approaches with normal commuter traffic
        // North Approach (2 vehicles)
        intersection.getRoad(Direction.NORTH).addVehicle(new Car("CAR-N1", 45, 2));
        intersection.getRoad(Direction.NORTH).addVehicle(new Bus("BUS-N1", 30, 42));

        // East Approach (2 vehicles)
        intersection.getRoad(Direction.EAST).addVehicle(new Bike("BIKE-E1", 25, true));
        intersection.getRoad(Direction.EAST).addVehicle(new Car("CAR-E1", 50, 4));

        // South Approach (2 vehicles)
        intersection.getRoad(Direction.SOUTH).addVehicle(new Car("CAR-S1", 40, 1));
        intersection.getRoad(Direction.SOUTH).addVehicle(new Bus("BUS-S1", 35, 30));

        // West Approach (2 vehicles)
        intersection.getRoad(Direction.WEST).addVehicle(new Car("CAR-W1", 55, 3));
        intersection.getRoad(Direction.WEST).addVehicle(new Bike("BIKE-W1", 20, false));

        // 2. Initialize SimulationEngine with 4s Green, 2s Yellow, 15s Starvation threshold
        // Note: The engine's default starvation threshold is 20s; configured here to 15s for compact demo.
        SimulationEngine engine = new SimulationEngine(intersection, 4, 2, 15);

        System.out.println("Intersection configured: Green=4s, Yellow=2s, StarvationThreshold=15s (default is 20s).");
        engine.printStatus();

        // 3. Normal Signal Progression (North is GREEN)
        // Run 2 ticks so North remains actively GREEN with 2 seconds remaining
        System.out.println("\n>>> [ACT 2] NORMAL ROUND-ROBIN CYCLING (NORTH APPROACH IS GREEN) <<<");
        for (int i = 1; i <= 2; i++) {
            engine.tick();
            engine.printStatus();
        }

        // 4. Inject Emergency Vehicles while North is actively GREEN
        System.out.println("\n>>> [ACT 3] EMERGENCY VEHICLE ARRIVAL WHILE NORTH IS ACTIVELY GREEN <<<");
        Ambulance ambEast = new Ambulance("AMB-E1", 70, 3, "St. Jude Hospital");
        Ambulance ambSouth = new Ambulance("AMB-S1", 85, 5, "City Emergency Trauma Center");

        System.out.println("-> AMBULANCE ARRIVED on East Approach: [AMB-E1, Urgency: 3/5, Dest: St. Jude Hospital]");
        System.out.println("-> AMBULANCE ARRIVED on South Approach: [AMB-S1, Urgency: 5/5, Dest: City Emergency Trauma Center]");
        System.out.println("Note: North Approach is currently GREEN (2s left). The engine will NOT abruptly switch South to GREEN.");
        System.out.println("Instead, North will safely transition through YELLOW before South receives GREEN.");

        // Add ambulances to the queues behind ordinary vehicles
        intersection.getRoad(Direction.EAST).addVehicle(ambEast);
        intersection.getRoad(Direction.SOUTH).addVehicle(ambSouth);

        // 5. Demonstrate Safe Preemption & Urgency Prioritization
        System.out.println("\n>>> [ACT 4] SAFE PREEMPTION: GREEN -> YELLOW -> RED -> HIGHEST-URGENCY GREEN <<<");
        // Tick 3: North curtails GREEN and switches to YELLOW
        // Tick 4: North counts down YELLOW (1s left)
        // Tick 5: North yellow expires, turns RED; South (Urgency 5 > 3) turns GREEN!
        // Tick 6: South is GREEN; AMB-S1 departs FIRST (bypassing ordinary cars via Queue Priority)!
        for (int i = 1; i <= 6; i++) {
            engine.tick();
            engine.printStatus();
        }

        // 6. Demonstrate Resuming Remaining Traffic & Starvation Protection
        System.out.println("\n>>> [ACT 5] CLEARING SECOND EMERGENCY (EAST) & RESUMING NORMAL ROUND-ROBIN <<<");
        for (int i = 1; i <= 8; i++) {
            engine.tick();
            engine.printStatus();
        }

        // Final Statistics Summary
        System.out.println("\n===================================================================================");
        System.out.println("  PHASE 3 CONSOLE SIMULATION SUMMARY");
        System.out.println("===================================================================================");
        System.out.println("  Total Simulation Ticks Elapsed : " + engine.getTotalTicks() + " seconds");
        System.out.println("  Total Vehicles Served          : " + engine.getTotalVehiclesServed());
        System.out.println("  North Remaining Queue          : " + intersection.getRoad(Direction.NORTH).getQueueLength());
        System.out.println("  East Remaining Queue           : " + intersection.getRoad(Direction.EAST).getQueueLength());
        System.out.println("  South Remaining Queue          : " + intersection.getRoad(Direction.SOUTH).getQueueLength());
        System.out.println("  West Remaining Queue           : " + intersection.getRoad(Direction.WEST).getQueueLength());
        System.out.println("===================================================================================\n");

        // =========================================================================
        // Phase 4: Report Generation & File I/O
        // =========================================================================
        System.out.println("===================================================================================");
        System.out.println("  PHASE 4: FILE HANDLING & REPORT GENERATION");
        System.out.println("===================================================================================");

        ReportService reportService = new ReportService();
        String reportFileName = "traffic_report.txt";

        try {
            reportService.generateReport(engine, reportFileName);
            File reportFile = new File(reportFileName);

            System.out.println("-> SUCCESS: Simulation report successfully generated!");
            System.out.println("-> Target File Name : " + reportFileName);
            System.out.println("-> Absolute Path    : " + reportFile.getAbsolutePath());
            System.out.println("-> File Size        : " + reportFile.length() + " bytes");
            System.out.println("-----------------------------------------------------------------------------------");
            System.out.println("-> Reading back saved report via FileReader & BufferedReader:\n");

            reportService.displayReport(reportFileName);

        } catch (ReportException e) {
            System.err.println("Report Generation Failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Root Cause: " + e.getCause().getMessage());
            }
        }

        // =========================================================================
        // Phase 5: Multithreading Demonstration (Runnable & Background Thread)
        // =========================================================================
        System.out.println("===================================================================================");
        System.out.println("  PHASE 5: MULTITHREADING CONTROLLER DEMONSTRATION");
        System.out.println("===================================================================================");

        Intersection asyncIntersection = new Intersection();
        asyncIntersection.getRoad(Direction.NORTH).addVehicle(new Car("ASYNC-CAR-1", 45, 2));
        asyncIntersection.getRoad(Direction.NORTH).addVehicle(new Car("ASYNC-CAR-2", 50, 1));
        asyncIntersection.getRoad(Direction.SOUTH).addVehicle(new Ambulance("ASYNC-AMB-1", 85, 5, "City Hospital"));

        SimulationEngine asyncEngine = new SimulationEngine(asyncIntersection, 2, 1, 10);
        SimulationController controller = new SimulationController(asyncEngine, 100);

        System.out.println("-> Starting background worker thread (Runnable) with 100ms tick interval...");
        controller.start();
        System.out.println("-> Thread Name   : " + controller.getWorkerThread().getName());
        System.out.println("-> Thread Alive  : " + controller.getWorkerThread().isAlive());
        System.out.println("-> Thread State  : " + controller.getWorkerThread().getState());

        try {
            // Let simulation run in background for 350ms (~3 ticks)
            Thread.sleep(350);
            System.out.println("-> Ticks after running (~350ms): " + asyncEngine.getTotalTicks()
                    + " (Vehicles served: " + asyncEngine.getTotalVehiclesServed() + ")");

            // Demonstrate Pause
            System.out.println("-> Pausing background thread...");
            controller.pause();
            int pausedTicks = asyncEngine.getTotalTicks();
            Thread.sleep(250);
            System.out.println("-> Ticks after 250ms paused : " + asyncEngine.getTotalTicks()
                    + " (Unchanged: " + (pausedTicks == asyncEngine.getTotalTicks()) + ")");

            // Demonstrate Resume
            System.out.println("-> Resuming background thread...");
            controller.resume();
            Thread.sleep(250);
            System.out.println("-> Ticks after 250ms resumed: " + asyncEngine.getTotalTicks()
                    + " (Progression resumed: " + (asyncEngine.getTotalTicks() > pausedTicks) + ")");

            // Demonstrate Safe Termination
            System.out.println("-> Stopping background thread safely...");
            controller.stop();
            controller.join(1000);
            System.out.println("-> Thread Alive after stop : " + controller.getWorkerThread().isAlive());
            System.out.println("-> Final Controller State  : "
                    + (controller.isRunning() ? "RUNNING" : "STOPPED (TERMINATED)"));
            System.out.println("===================================================================================");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted during demonstration: " + e.getMessage());
        }
    }
}

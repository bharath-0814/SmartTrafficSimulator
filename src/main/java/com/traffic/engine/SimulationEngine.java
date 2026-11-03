package com.traffic.engine;

import com.traffic.model.Ambulance;
import com.traffic.model.Bike;
import com.traffic.model.Bus;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.EmergencyCapable;
import com.traffic.model.Intersection;
import com.traffic.model.Road;
import com.traffic.model.SignalColor;
import com.traffic.model.TrafficSignal;
import com.traffic.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Core Traffic Simulation Engine for the 4-way intersection.
 *
 * Responsibilities:
 * 1. Manages discrete simulation ticks (advancing time by 1 second).
 * 2. Implements round-robin signal rotation: NORTH -> EAST -> SOUTH -> WEST.
 * 3. Enforces single-green safety invariant: only 1 approach may be GREEN at a time.
 * 4. Executes safe signal transitions: GREEN -> YELLOW -> RED -> next road GREEN.
 * 5. Prioritizes emergency vehicles (ambulances) by highest urgency without abrupt switches.
 * 6. Prevents starvation of regular traffic by enforcing a maximum waiting threshold.
 * 7. Tracks simulation statistics (ticks elapsed, vehicles served, waiting times).
 *
 * OOP Concepts Demonstrated:
 * - Encapsulation: Internal timing and state are protected behind clean methods.
 * - Polymorphism: Handles any Vehicle subclass uniformly through FIFO queues.
 * - Composition: Coordinates Intersection, Roads, and TrafficSignals.
 */
public class SimulationEngine {

    public static final int DEFAULT_GREEN_DURATION = 5;
    public static final int DEFAULT_YELLOW_DURATION = 2;
    /**
     * Default starvation threshold: 20 seconds.
     * Prevents ordinary commuter traffic (Cars, Bikes, Buses) from waiting indefinitely
     * when recurring emergency vehicles trigger preemptions.
     * (Note: The interactive console demonstration in Main.java explicitly configures this
     * threshold to 15 seconds to demonstrate anti-starvation in a compact terminal run).
     */
    public static final int DEFAULT_STARVATION_THRESHOLD = 20;

    private Intersection intersection;
    private int currentGreenIndex;       // 0=NORTH, 1=EAST, 2=SOUTH, 3=WEST
    private int greenDuration;
    private int yellowDuration;
    private int starvationThreshold;
    private int totalVehiclesServed;
    private int totalEmergencyVehiclesServed;
    private int totalTicks;
    private String lastEventDescription;

    /**
     * Parameterized Constructor allowing full configuration.
     *
     * @param intersection       The 4-way intersection to simulate.
     * @param greenDuration      Seconds for green signal phase.
     * @param yellowDuration     Seconds for yellow transition phase.
     * @param starvationThreshold Max waiting time (seconds) before normal traffic is prioritized.
     */
    public SimulationEngine(Intersection intersection, int greenDuration, int yellowDuration, int starvationThreshold) {
        if (intersection == null) {
            throw new IllegalArgumentException("Intersection cannot be null.");
        }
        if (greenDuration <= 0) {
            throw new IllegalArgumentException("Green duration must be positive: " + greenDuration);
        }
        if (yellowDuration <= 0) {
            throw new IllegalArgumentException("Yellow duration must be positive: " + yellowDuration);
        }
        if (starvationThreshold <= 0) {
            throw new IllegalArgumentException("Starvation threshold must be positive: " + starvationThreshold);
        }

        this.intersection = intersection;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.starvationThreshold = starvationThreshold;
        this.totalVehiclesServed = 0;
        this.totalEmergencyVehiclesServed = 0;
        this.totalTicks = 0;
        this.lastEventDescription = "Simulation initialized.";

        // Configure signal durations on all approaches and ensure all start RED
        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            road.getSignal().setGreenDuration(greenDuration);
            road.getSignal().setYellowDuration(yellowDuration);
            road.getSignal().switchToRed();
        }

        // Initialize NORTH (Index 0) as the starting GREEN road
        this.currentGreenIndex = 0;
        Road startingRoad = intersection.getRoad(currentGreenIndex);
        startingRoad.getSignal().switchToGreen();
    }

    /**
     * Overloaded Constructor with custom green and yellow durations.
     */
    public SimulationEngine(Intersection intersection, int greenDuration, int yellowDuration) {
        this(intersection, greenDuration, yellowDuration, DEFAULT_STARVATION_THRESHOLD);
    }

    /**
     * Overloaded Constructor with default timing parameters.
     */
    public SimulationEngine(Intersection intersection) {
        this(intersection, DEFAULT_GREEN_DURATION, DEFAULT_YELLOW_DURATION, DEFAULT_STARVATION_THRESHOLD);
    }

    // =========================================================================
    // Core Simulation Tick Logic
    // =========================================================================

    /**
     * Advances the simulation by exactly one discrete time tick (1 second).
     * Synchronized to guarantee atomic state progression during multithreaded simulation.
     *
     * In each tick:
     * 1. Evaluates emergency preemption and signal timers.
     * 2. Allows 1 vehicle from the active GREEN road to pass through.
     * 3. Increments waiting times for vehicles waiting on RED roads.
     * 4. Updates statistics and logs the event.
     */
    public synchronized void tick() {
        totalTicks++;
        lastEventDescription = "Normal traffic flow.";

        Road activeRoad = intersection.getRoad(currentGreenIndex);
        TrafficSignal activeSignal = activeRoad.getSignal();

        // -------------------------------------------------------------
        // Step 1: Check for Emergency Preemption
        // -------------------------------------------------------------
        int emergencyRoadIdx = findHighestUrgencyRoadIndex();
        boolean emergencyOnOtherRoad = (emergencyRoadIdx != -1 && emergencyRoadIdx != currentGreenIndex);
        boolean starvationActive = (findStarvedRoadIndex() != -1);

        // If an emergency is waiting on another road AND no road is currently starving:
        // Safely curtail the active green phase by switching immediately to YELLOW
        if (emergencyOnOtherRoad && !starvationActive && activeSignal.isGreen()) {
            activeSignal.switchToYellow();
            lastEventDescription = "EMERGENCY PREEMPTION: " + activeRoad.getName()
                    + " switched to YELLOW for incoming emergency on "
                    + intersection.getRoad(emergencyRoadIdx).getName() + ".";
        } else {
            // ---------------------------------------------------------
            // Step 2: Normal Signal Countdown & Phase Transitions
            // ---------------------------------------------------------
            // If the signal is green, a vehicle may pass BEFORE the timer decrements
            if (activeSignal.isGreen() && !activeRoad.isEmpty()) {
                Vehicle departing = activeRoad.removeVehicle();
                if (departing != null) {
                    totalVehiclesServed++;
                    if (departing.isEmergency()) {
                        totalEmergencyVehiclesServed++;
                    }
                    lastEventDescription = departing.getId() + " passed through " + activeRoad.getName() + ".";
                }
            }

            activeSignal.tick();

            // Handle transition when current phase timer reaches zero
            if (activeSignal.isTimerExpired()) {
                if (activeSignal.isGreen()) {
                    // Sequence step: GREEN -> YELLOW
                    activeSignal.switchToYellow();
                    lastEventDescription = activeRoad.getName() + " green phase ended; switched to YELLOW.";
                } else if (activeSignal.isYellow()) {
                    // Sequence step: YELLOW -> RED -> next road GREEN
                    activeSignal.switchToRed();
                    activeSignal.setEmergencyOverride(false);

                    // Select the next approach according to scheduling rules
                    int nextIndex = determineNextRoadIndex();
                    currentGreenIndex = nextIndex;

                    Road nextRoad = intersection.getRoad(currentGreenIndex);
                    nextRoad.getSignal().switchToGreen();

                    if (nextRoad.hasEmergencyVehicle()) {
                        nextRoad.getSignal().setEmergencyOverride(true);
                        lastEventDescription = "EMERGENCY PRIORITY: " + nextRoad.getName() + " granted GREEN.";
                    } else {
                        lastEventDescription = nextRoad.getName() + " granted GREEN.";
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Step 3: Increment Waiting Time for Vehicles on RED Roads Only
        // -------------------------------------------------------------
        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            // Vehicles only accumulate waiting time when stopped at a RED signal.
            // Moving vehicles on GREEN roads do not accumulate red-light waiting time.
            if (road.getSignal().isRed()) {
                ArrayList<Vehicle> vehicles = road.getVehicles();
                for (Vehicle v : vehicles) {
                    v.incrementWaitingTime();
                }
            }
        }
    }

    /**
     * Advances the simulation by multiple ticks (Thread-safe).
     *
     * @param numberOfTicks Number of seconds to simulate.
     */
    public synchronized void step(int numberOfTicks) {
        if (numberOfTicks <= 0) {
            throw new IllegalArgumentException("Number of ticks must be positive: " + numberOfTicks);
        }
        for (int i = 0; i < numberOfTicks; i++) {
            tick();
        }
    }

    // =========================================================================
    // Decision Logic: Emergency Prioritization & Starvation Prevention
    // =========================================================================

    /**
     * Determines which approach should turn GREEN next.
     * Priority Hierarchy:
     * 1. Anti-Starvation: Serve road with the longest waiting normal vehicles.
     * 2. Emergency Priority: Serve road with the highest-urgency emergency vehicle.
     * 3. Normal Round-Robin: Rotate sequentially (NORTH -> EAST -> SOUTH -> WEST).
     */
    private int determineNextRoadIndex() {
        // Priority 1: Starvation prevention
        int starvedIndex = findStarvedRoadIndex();
        if (starvedIndex != -1) {
            return starvedIndex;
        }

        // Priority 2: Emergency priority (highest urgency ambulance)
        int emergencyIndex = findHighestUrgencyRoadIndex();
        if (emergencyIndex != -1) {
            return emergencyIndex;
        }

        // Priority 3: Normal cyclic round-robin
        return (currentGreenIndex + 1) % intersection.getRoadCount();
    }

    /**
     * Finds the road index containing the highest urgency emergency vehicle.
     * Returns -1 if no emergency vehicle is currently waiting on any approach.
     */
    public int findHighestUrgencyRoadIndex() {
        int bestIndex = -1;
        int highestUrgency = 0;

        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            int urgency = getHighestUrgencyOnRoad(road);
            if (urgency > highestUrgency) {
                highestUrgency = urgency;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Inspects a single road and returns the highest urgency level among its waiting vehicles.
     * Returns 0 if there are no emergency vehicles.
     */
    public int getHighestUrgencyOnRoad(Road road) {
        if (road == null) {
            return 0;
        }
        int maxUrgency = 0;
        ArrayList<Vehicle> vehicles = road.getVehicles();
        for (Vehicle v : vehicles) {
            if (v instanceof EmergencyCapable) {
                EmergencyCapable ev = (EmergencyCapable) v;
                if (ev.getUrgencyLevel() > maxUrgency) {
                    maxUrgency = ev.getUrgencyLevel();
                }
            }
        }
        return maxUrgency;
    }

    /**
     * Checks if any approach has ordinary (non-emergency) commuter vehicles that have
     * waited at or beyond the starvation threshold.
     *
     * Rule: Starvation prevention applies strictly to regular commuter vehicles
     * (Cars, Bikes, Buses) to prevent them from waiting indefinitely.
     * Emergency vehicles are prioritized independently via findHighestUrgencyRoadIndex().
     *
     * @return The index of the road with the longest-starved ordinary vehicles, or -1 if none.
     */
    public int findStarvedRoadIndex() {
        int starvedIndex = -1;
        int maxWait = -1;

        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            if (!road.isEmpty()) {
                int ordinaryWait = getMaxOrdinaryWaitingTimeOnRoad(road);
                if (ordinaryWait >= starvationThreshold && ordinaryWait > maxWait) {
                    maxWait = ordinaryWait;
                    starvedIndex = i;
                }
            }
        }
        return starvedIndex;
    }

    /**
     * Returns the maximum waiting time (in seconds) among all ORDINARY (non-emergency)
     * vehicles on a road. Emergency vehicles are excluded so that starvation tracking
     * measures only commuter delays.
     */
    public int getMaxOrdinaryWaitingTimeOnRoad(Road road) {
        if (road == null) {
            return 0;
        }
        int max = 0;
        ArrayList<Vehicle> vehicles = road.getVehicles();
        for (Vehicle v : vehicles) {
            if (!v.isEmergency()) {
                if (v.getWaitingTime() > max) {
                    max = v.getWaitingTime();
                }
            }
        }
        return max;
    }

    /**
     * Returns the maximum waiting time (in seconds) among all vehicles on a road.
     */
    public int getMaxWaitingTimeOnRoad(Road road) {
        if (road == null) {
            return 0;
        }
        int max = 0;
        ArrayList<Vehicle> vehicles = road.getVehicles();
        for (Vehicle v : vehicles) {
            if (v.getWaitingTime() > max) {
                max = v.getWaitingTime();
            }
        }
        return max;
    }

    // =========================================================================
    // Inspection & State Accessors (Thread-safe)
    // =========================================================================

    /**
     * Safely adds a vehicle to an approach road by index (Thread-safe).
     * Synchronized on the engine to avoid race conditions with tick().
     *
     * @param roadIndex Approach index (0=North, 1=East, 2=South, 3=West).
     * @param vehicle The vehicle to add.
     * @return true if vehicle was successfully added, false if road is full or index invalid.
     */
    public synchronized boolean addVehicle(int roadIndex, Vehicle vehicle) {
        if (vehicle == null || roadIndex < 0 || roadIndex >= intersection.getRoadCount()) {
            return false;
        }
        return intersection.getRoad(roadIndex).addVehicle(vehicle);
    }

    /**
     * Safely adds a vehicle to an approach road by direction (Thread-safe).
     *
     * @param direction Compass direction of the incoming approach road.
     * @param vehicle The vehicle to add.
     * @return true if vehicle was successfully added, false if road is full or direction null.
     */
    public synchronized boolean addVehicle(Direction direction, Vehicle vehicle) {
        if (vehicle == null || direction == null) {
            return false;
        }
        Road road = intersection.getRoad(direction);
        if (road == null) {
            return false;
        }
        return road.addVehicle(vehicle);
    }

    /**
     * Captures an atomic, immutable snapshot of the entire simulation state.
     * Guaranteed to be consistent because all values and queues are read in one
     * synchronized execution.
     *
     * @return Immutable SimulationSnapshot for safe GUI rendering and reporting.
     */
    public synchronized SimulationSnapshot getSnapshot() {
        List<SimulationSnapshot.RoadSnapshot> roadSnapshots = new ArrayList<>();
        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            TrafficSignal signal = road.getSignal();

            List<SimulationSnapshot.VehicleSnapshot> vehicleSnapshots = new ArrayList<>();
            ArrayList<Vehicle> vehicles = road.getVehicles();
            for (Vehicle v : vehicles) {
                int urgency = 0;
                if (v instanceof EmergencyCapable) {
                    urgency = ((EmergencyCapable) v).getUrgencyLevel();
                }
                vehicleSnapshots.add(new SimulationSnapshot.VehicleSnapshot(
                        v.getId(),
                        v.getClass().getSimpleName(),
                        v.getSpeed(),
                        v.getWaitingTime(),
                        v.isEmergency(),
                        v instanceof Ambulance,
                        v instanceof Bus,
                        v instanceof Bike,
                        v instanceof Car,
                        urgency
                ));
            }

            roadSnapshots.add(new SimulationSnapshot.RoadSnapshot(
                    road.getName(),
                    road.getDirection(),
                    signal.getColor(),
                    signal.getTimer(),
                    signal.isEmergencyOverride(),
                    road.getMaxCapacity(),
                    road.getQueueLength(),
                    vehicleSnapshots
            ));
        }

        return new SimulationSnapshot(
                totalTicks,
                totalVehiclesServed,
                totalEmergencyVehiclesServed,
                currentGreenIndex,
                greenDuration,
                yellowDuration,
                starvationThreshold,
                isEmergencyActive(),
                lastEventDescription,
                roadSnapshots
        );
    }

    /**
     * Returns the underlying Intersection model.
     * Note: Prefer using getSnapshot() for UI rendering and reporting to avoid
     * exposing mutable simulation state across threads.
     */
    public synchronized Intersection getIntersection() {
        return intersection;
    }

    public synchronized int getCurrentGreenIndex() {
        return currentGreenIndex;
    }

    public synchronized Road getCurrentActiveRoad() {
        return intersection.getRoad(currentGreenIndex);
    }

    public synchronized int getTotalVehiclesServed() {
        return totalVehiclesServed;
    }

    public synchronized int getTotalEmergencyVehiclesServed() {
        return totalEmergencyVehiclesServed;
    }

    public synchronized int getTotalTicks() {
        return totalTicks;
    }

    public synchronized int getGreenDuration() {
        return greenDuration;
    }

    public synchronized int getYellowDuration() {
        return yellowDuration;
    }

    public synchronized int getStarvationThreshold() {
        return starvationThreshold;
    }

    public synchronized void setStarvationThreshold(int starvationThreshold) {
        if (starvationThreshold <= 0) {
            throw new IllegalArgumentException("Starvation threshold must be positive: " + starvationThreshold);
        }
        this.starvationThreshold = starvationThreshold;
    }

    public synchronized String getLastEventDescription() {
        return lastEventDescription;
    }

    /**
     * Checks if any approach is currently in an active emergency override state (Thread-safe).
     */
    public synchronized boolean isEmergencyActive() {
        for (int i = 0; i < intersection.getRoadCount(); i++) {
            if (intersection.getRoad(i).getSignal().isEmergencyOverride()) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Console Formatting & Display
    // =========================================================================

    /**
     * Prints a formatted status table of the intersection to the console.
     */
    public void printStatus() {
        Road activeRoad = getCurrentActiveRoad();
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.printf("| TICK: %03d | Served: %-3d | Active Road: %-16s | Override: %-5s |\n",
                totalTicks, totalVehiclesServed, activeRoad.getName(), (isEmergencyActive() ? "YES" : "NO"));
        System.out.printf("| Event: %-74s |\n", truncateString(lastEventDescription, 74));
        System.out.println("+-----------+-----------------+----------------+-------+-------+--------------------+");
        System.out.println("| Direction | Road Name       | Signal Light   | Timer | Queue | Waiting Vehicles   |");
        System.out.println("+-----------+-----------------+----------------+-------+-------+--------------------+");

        for (int i = 0; i < intersection.getRoadCount(); i++) {
            Road road = intersection.getRoad(i);
            TrafficSignal signal = road.getSignal();

            String queueIds = formatQueueIds(road);
            System.out.printf("| %-9s | %-15s | %-14s | %4ds | %5d | %-18s |\n",
                    road.getDirection(),
                    truncateString(road.getName(), 15),
                    formatSignalStatus(signal),
                    signal.getTimer(),
                    road.getQueueLength(),
                    truncateString(queueIds, 18));
        }
        System.out.println("+-----------+-----------------+----------------+-------+-------+--------------------+");
    }

    private String formatSignalStatus(TrafficSignal signal) {
        if (signal.isGreen()) {
            return "[GREEN]";
        } else if (signal.isYellow()) {
            return "[YELLOW]";
        } else {
            return "RED";
        }
    }

    private String formatQueueIds(Road road) {
        ArrayList<Vehicle> vehicles = road.getVehicles();
        if (vehicles.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vehicles.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vehicles.get(i).getId());
        }
        return sb.toString();
    }

    private String truncateString(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}

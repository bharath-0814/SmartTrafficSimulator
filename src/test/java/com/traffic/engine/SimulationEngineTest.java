package com.traffic.engine;

import com.traffic.model.Ambulance;
import com.traffic.model.Bike;
import com.traffic.model.Bus;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import com.traffic.model.Road;
import com.traffic.model.SignalColor;
import com.traffic.model.TrafficSignal;
import com.traffic.model.Vehicle;
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
 * Comprehensive JUnit 5 Test Suite for SimulationEngine (Phase 3).
 *
 * Verifies:
 * 1. Initial engine configuration and default values.
 * 2. Critical Safety Invariant: At no point are two roads simultaneously GREEN.
 * 3. Exact Signal Transition Sequence: GREEN -> YELLOW -> RED -> next GREEN.
 * 4. Normal cyclic round-robin rotation: NORTH -> EAST -> SOUTH -> WEST.
 * 5. Emergency Prioritization: Highest-urgency ambulance receives priority.
 * 6. Safe Preemption: Green road transitions through YELLOW before emergency road gets green.
 * 7. Starvation Prevention: Long-waiting commuter traffic gets served even when emergencies exist.
 * 8. Queue Management & Statistics: Departures decrement queues and increment total served.
 */
class SimulationEngineTest {

    private Intersection intersection;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
        // Green=3s, Yellow=2s, StarvationThreshold=10s
        engine = new SimulationEngine(intersection, 3, 2, 10);
    }

    @Test
    @DisplayName("Test 1: Engine initialization and default values")
    void testEngineInitialization() {
        assertEquals(0, engine.getTotalTicks(), "Initial ticks should be 0");
        assertEquals(0, engine.getTotalVehiclesServed(), "Initial served vehicles should be 0");
        assertEquals(0, engine.getCurrentGreenIndex(), "NORTH (index 0) should be active initially");

        // North is GREEN, others are RED
        assertTrue(intersection.getRoad(Direction.NORTH).getSignal().isGreen(), "North should be GREEN");
        assertTrue(intersection.getRoad(Direction.EAST).getSignal().isRed(), "East should be RED");
        assertTrue(intersection.getRoad(Direction.SOUTH).getSignal().isRed(), "South should be RED");
        assertTrue(intersection.getRoad(Direction.WEST).getSignal().isRed(), "West should be RED");
    }

    @Test
    @DisplayName("Test 2: Critical Invariant: Two roads are NEVER simultaneously GREEN")
    void testNeverSimultaneousGreen() {
        // Run simulation for 100 consecutive ticks
        for (int tick = 1; tick <= 100; tick++) {
            engine.tick();

            int greenCount = 0;
            for (int r = 0; r < intersection.getRoadCount(); r++) {
                if (intersection.getRoad(r).getSignal().isGreen()) {
                    greenCount++;
                }
            }

            // Invariant: At most 1 road can be GREEN at any moment (0 during pure yellow phases)
            assertTrue(greenCount <= 1,
                    "Safety violation at tick " + tick + ": " + greenCount + " roads are simultaneously GREEN!");
        }
    }

    @Test
    @DisplayName("Test 3: Signal transition sequence GREEN -> YELLOW -> RED -> next GREEN")
    void testSignalTransitionSequence() {
        Road north = intersection.getRoad(Direction.NORTH);
        Road east = intersection.getRoad(Direction.EAST);

        // Tick 0: North is GREEN (timer 3)
        assertTrue(north.getSignal().isGreen());
        assertEquals(3, north.getSignal().getTimer());

        // Tick 1: North timer 3 -> 2
        engine.tick();
        assertTrue(north.getSignal().isGreen());
        assertEquals(2, north.getSignal().getTimer());

        // Tick 2: North timer 2 -> 1
        engine.tick();
        assertTrue(north.getSignal().isGreen());
        assertEquals(1, north.getSignal().getTimer());

        // Tick 3: North timer 1 -> 0 => Transitions to YELLOW (timer 2)
        engine.tick();
        assertTrue(north.getSignal().isYellow(), "North must transition from GREEN to YELLOW");
        assertEquals(2, north.getSignal().getTimer());

        // Tick 4: North is in YELLOW (timer 2 -> 1)
        engine.tick();
        assertTrue(north.getSignal().isYellow());
        assertEquals(1, north.getSignal().getTimer());

        // Tick 5: North yellow expires => North turns RED, East turns GREEN!
        engine.tick();
        assertTrue(north.getSignal().isRed(), "North must transition from YELLOW to RED");
        assertTrue(east.getSignal().isGreen(), "East must transition to GREEN after North turns RED");
        assertEquals(1, engine.getCurrentGreenIndex(), "East (index 1) must now be current green index");
    }

    @Test
    @DisplayName("Test 4: Normal round-robin rotation sequence (NORTH -> EAST -> SOUTH -> WEST -> NORTH)")
    void testNormalRoundRobinRotation() {
        // North starts GREEN (index 0)
        assertEquals(0, engine.getCurrentGreenIndex());

        // Advance through North (3 green + 2 yellow = 5 ticks)
        engine.step(5);
        assertEquals(1, engine.getCurrentGreenIndex(), "Should advance to EAST (index 1)");
        assertTrue(intersection.getRoad(Direction.EAST).getSignal().isGreen());

        // Advance through East (5 ticks)
        engine.step(5);
        assertEquals(2, engine.getCurrentGreenIndex(), "Should advance to SOUTH (index 2)");
        assertTrue(intersection.getRoad(Direction.SOUTH).getSignal().isGreen());

        // Advance through South (5 ticks)
        engine.step(5);
        assertEquals(3, engine.getCurrentGreenIndex(), "Should advance to WEST (index 3)");
        assertTrue(intersection.getRoad(Direction.WEST).getSignal().isGreen());

        // Advance through West (5 ticks)
        engine.step(5);
        assertEquals(0, engine.getCurrentGreenIndex(), "Should wrap around to NORTH (index 0)");
        assertTrue(intersection.getRoad(Direction.NORTH).getSignal().isGreen());
    }

    @Test
    @DisplayName("Test 5: Safe preemption curtails GREEN to YELLOW rather than abrupt switch")
    void testSafePreemptionTransitionsThroughYellow() {
        Road north = intersection.getRoad(Direction.NORTH);
        Road south = intersection.getRoad(Direction.SOUTH);

        // North has 3 seconds green
        assertTrue(north.getSignal().isGreen());

        // Add Ambulance to South
        Ambulance amb = new Ambulance("AMB-S1", 70, 5, "Trauma Center");
        south.addVehicle(amb);

        // Advance 1 tick: North detects emergency on South
        engine.tick();

        // North must NOT abruptly jump to RED, and South must NOT abruptly jump to GREEN
        assertTrue(north.getSignal().isYellow(), "Active road must switch to YELLOW to allow vehicles to stop safely");
        assertFalse(south.getSignal().isGreen(), "Emergency road must not turn GREEN before active road clears yellow");

        // Advance 2 ticks (yellow duration)
        engine.tick();
        engine.tick();

        // Now North is RED and South is granted GREEN
        assertTrue(north.getSignal().isRed(), "North should now be RED");
        assertTrue(south.getSignal().isGreen(), "South should now be GREEN for the ambulance");
        assertEquals(2, engine.getCurrentGreenIndex(), "South index (2) should be active");
    }

    @Test
    @DisplayName("Test 6: Highest urgency ambulance gets priority between multiple ambulances")
    void testHighestUrgencyAmbulanceGetsPriority() {
        Road east = intersection.getRoad(Direction.EAST);
        Road south = intersection.getRoad(Direction.SOUTH);

        // East has urgency 3 ambulance
        Ambulance ambEast = new Ambulance("AMB-E1", 60, 3, "East Clinic");
        east.addVehicle(ambEast);

        // South has urgency 5 ambulance (more critical)
        Ambulance ambSouth = new Ambulance("AMB-S1", 80, 5, "City Trauma Center");
        south.addVehicle(ambSouth);

        assertEquals(2, engine.findHighestUrgencyRoadIndex(), "South (index 2) should have the highest urgency (5 > 3)");

        // Advance simulation until active road transitions
        // North starts GREEN -> switches to YELLOW -> RED
        engine.step(3); // Preemption yellow + completion
        engine.step(1);

        // Verify that SOUTH (urgency 5) was selected over EAST (urgency 3)
        assertEquals(2, engine.getCurrentGreenIndex(), "South (urgency 5) must be prioritized over East (urgency 3)");
        assertTrue(south.getSignal().isGreen());
    }

    @Test
    @DisplayName("Test 7: Starvation prevention ensures normal traffic eventually gets served")
    void testStarvationPrevention() {
        Road west = intersection.getRoad(Direction.WEST);
        Road south = intersection.getRoad(Direction.SOUTH);

        // Put a normal car on West
        Car normalCar = new Car("CAR-W1", 50, 2);
        west.addVehicle(normalCar);

        // Artificially increase West car waiting time to trigger starvation threshold (10s)
        normalCar.setWaitingTime(12);

        // Even though South has an ambulance, West is starving (12s >= 10s threshold)
        Ambulance ambSouth = new Ambulance("AMB-S1", 80, 5, "Hospital");
        south.addVehicle(ambSouth);

        // Advance North through its full green (3s) and yellow (2s) phases to red (5 ticks total)
        engine.step(5);

        // West must be served to prevent permanent starvation of regular commuters
        assertEquals(3, engine.getCurrentGreenIndex(), "Starved road (West, index 3) must be served first");
        assertTrue(west.getSignal().isGreen(), "West must be granted GREEN to prevent starvation");
    }

    @Test
    @DisplayName("Test 8: Vehicle queues and statistics update correctly on departure")
    void testVehicleQueuesAndStatisticsUpdate() {
        Road north = intersection.getRoad(Direction.NORTH);
        Road east = intersection.getRoad(Direction.EAST);

        north.addVehicle(new Car("CAR-N1", 45, 2));
        north.addVehicle(new Bike("BIKE-N1", 30, true));
        east.addVehicle(new Car("CAR-E1", 50, 4));

        assertEquals(2, north.getQueueLength());
        assertEquals(1, east.getQueueLength());
        assertEquals(0, engine.getTotalVehiclesServed());

        // Tick 1: North is GREEN, 1st vehicle (CAR-N1) passes
        engine.tick();
        assertEquals(1, north.getQueueLength(), "North queue should decrease by 1");
        assertEquals(1, engine.getTotalVehiclesServed(), "Vehicles served should be 1");

        // Tick 2: North is GREEN, 2nd vehicle (BIKE-N1) passes
        engine.tick();
        assertEquals(0, north.getQueueLength(), "North queue should be empty");
        assertEquals(2, engine.getTotalVehiclesServed(), "Vehicles served should be 2");

        // Verify that vehicle waiting on RED East approach had its waiting time incremented
        Vehicle eastCar = east.getVehicle(0);
        assertNotNull(eastCar);
        assertEquals(2, eastCar.getWaitingTime(), "Waiting vehicle on red road should have waiting time 2");
    }

    @Test
    @DisplayName("Test 9: Constructor validation and illegal arguments")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationEngine(null, 5, 2, 10));
        assertThrows(IllegalArgumentException.class, () -> new SimulationEngine(intersection, -1, 2, 10));
        assertThrows(IllegalArgumentException.class, () -> new SimulationEngine(intersection, 5, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new SimulationEngine(intersection, 5, 2, -5));
        assertThrows(IllegalArgumentException.class, () -> engine.step(0));
        assertThrows(IllegalArgumentException.class, () -> engine.setStarvationThreshold(0));
    }

    @Test
    @DisplayName("Test 10: Emergency vehicle is served before ordinary vehicles waiting behind it on the same road")
    void testEmergencyVehicleServedBeforeOrdinaryVehiclesOnSameRoad() {
        Road northRoad = intersection.getRoad(Direction.NORTH);

        // Queue: Car -> Bus -> Ambulance (Ambulance is queued BEHIND ordinary vehicles)
        Car car = new Car("CAR-1", 40, 2);
        Bus bus = new Bus("BUS-1", 30, 30);
        Ambulance ambulance = new Ambulance("AMB-1", 80, 5, "General Hospital");

        northRoad.addVehicle(car);
        northRoad.addVehicle(bus);
        northRoad.addVehicle(ambulance);

        assertEquals(3, northRoad.getQueueLength());

        // 1. Ambulance must bypass ordinary vehicles and be served first
        Vehicle departed1 = northRoad.removeVehicle();
        assertNotNull(departed1);
        assertEquals("AMB-1", departed1.getId(), "Ambulance must be served before ordinary vehicles in its lane");
        assertTrue(departed1.isEmergency());
        assertEquals(2, northRoad.getQueueLength());

        // 2. Once emergency vehicles clear, ordinary vehicles depart in FIFO order
        Vehicle departed2 = northRoad.removeVehicle();
        assertEquals("CAR-1", departed2.getId(), "First ordinary vehicle (CAR-1) departs next");

        Vehicle departed3 = northRoad.removeVehicle();
        assertEquals("BUS-1", departed3.getId(), "Second ordinary vehicle (BUS-1) departs after CAR-1");

        assertTrue(northRoad.isEmpty());
    }

    @Test
    @DisplayName("Test 11: Ordinary traffic starvation works independently of emergency vehicle waiting time")
    void testOrdinaryTrafficStarvationIndependentOfEmergencyWaitTime() {
        Road south = intersection.getRoad(Direction.SOUTH);
        Road west = intersection.getRoad(Direction.WEST);

        // South has an ambulance that has been waiting for 35 seconds (well above the 10s threshold)
        Ambulance ambSouth = new Ambulance("AMB-S1", 80, 5, "Trauma Center");
        ambSouth.setWaitingTime(35);
        south.addVehicle(ambSouth);

        // West has an ordinary car that has only been waiting 2 seconds
        Car westCar = new Car("CAR-W1", 50, 1);
        westCar.setWaitingTime(2);
        west.addVehicle(westCar);

        // Assert that the ambulance's high waiting time does NOT falsely trigger ordinary traffic starvation
        assertEquals(-1, engine.findStarvedRoadIndex(),
                "Emergency vehicle wait time must not trigger the ordinary traffic starvation rule");
        assertEquals(0, engine.getMaxOrdinaryWaitingTimeOnRoad(south),
                "South road has only an ambulance, so ordinary waiting time should be 0");

        // Now artificially raise the ordinary commuter vehicle's wait time to 14s (>= 10s threshold)
        westCar.setWaitingTime(14);
        assertEquals(14, engine.getMaxOrdinaryWaitingTimeOnRoad(west));
        assertEquals(3, engine.findStarvedRoadIndex(),
                "Ordinary vehicle exceeding starvation threshold must trigger starvation for West approach");
    }

    @Test
    @DisplayName("Test 12: Emergency preemption happens when an ambulance arrives during an active GREEN phase")
    void testEmergencyPreemptionDuringActiveGreenPhase() {
        Road north = intersection.getRoad(Direction.NORTH);
        Road south = intersection.getRoad(Direction.SOUTH);

        // North starts GREEN with 3s green duration
        assertTrue(north.getSignal().isGreen());
        assertEquals(3, north.getSignal().getTimer());

        // Advance 1 tick: North is actively GREEN with 2s remaining
        engine.tick();
        assertTrue(north.getSignal().isGreen(), "North must be actively GREEN before emergency arrives");
        assertEquals(2, north.getSignal().getTimer());

        // Ambulance arrives on South while North is actively GREEN
        Ambulance amb = new Ambulance("AMB-S1", 80, 5, "Trauma Center");
        south.addVehicle(amb);

        // Next tick: Engine detects emergency and curtails North to YELLOW
        engine.tick();
        assertTrue(north.getSignal().isYellow(), "Active road must switch from GREEN to YELLOW upon preemption");
        assertFalse(south.getSignal().isGreen(), "Emergency road must NOT turn green until active road clears yellow");

        // Advance through yellow clearance phase (2 ticks)
        engine.tick(); // Yellow timer 2 -> 1
        engine.tick(); // Yellow timer 1 -> 0 => North turns RED, South turns GREEN!

        assertTrue(north.getSignal().isRed(), "North must turn RED after yellow phase expires");
        assertTrue(south.getSignal().isGreen(), "South must be granted GREEN after safe transition completes");
        assertEquals(2, engine.getCurrentGreenIndex(), "South (index 2) must be the active green approach");
    }
}

package com.traffic.engine;

import com.traffic.model.Ambulance;
import com.traffic.model.Bike;
import com.traffic.model.Bus;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import com.traffic.model.Road;
import com.traffic.model.SignalColor;
import com.traffic.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for {@link SimulationSnapshot} and thread-safe engine operations.
 *
 * Verifies:
 * 1. Snapshot captures a consistent point-in-time freeze-frame of all approaches and queues.
 * 2. Snapshot immutability: collections returned by the snapshot cannot be modified externally.
 * 3. Advancing the simulation does not mutate previously captured snapshots.
 * 4. Safe vehicle injection via {@link SimulationEngine#addVehicle(int, Vehicle)} and
 *    {@link SimulationEngine#addVehicle(Direction, Vehicle)}.
 * 5. Concurrent vehicle injection while the background simulation worker thread is actively ticking.
 */
class SimulationSnapshotTest {

    private Intersection intersection;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
        // Green=3s, Yellow=2s, StarvationThreshold=10s
        engine = new SimulationEngine(intersection, 3, 2, 10);
    }

    @Test
    @DisplayName("Test 1: Snapshot captures complete and consistent simulation metrics")
    void testSnapshotMetricsConsistency() {
        engine.addVehicle(Direction.NORTH, new Car("CAR-N1", 40, 2));
        engine.addVehicle(Direction.SOUTH, new Ambulance("AMB-S1", 75, 5, "Trauma Center"));

        SimulationSnapshot snapshot = engine.getSnapshot();

        assertNotNull(snapshot);
        assertEquals(0, snapshot.getTotalTicks());
        assertEquals(0, snapshot.getTotalVehiclesServed());
        assertEquals(0, snapshot.getTotalEmergencyVehiclesServed());
        assertEquals(0, snapshot.getCurrentGreenIndex());
        assertEquals(3, snapshot.getGreenDuration());
        assertEquals(2, snapshot.getYellowDuration());
        assertEquals(10, snapshot.getStarvationThreshold());
        assertNotNull(snapshot.getActiveRoad());
        assertEquals("North Approach", snapshot.getActiveRoad().getName());

        // Road checks
        assertEquals(4, snapshot.getRoads().size());
        SimulationSnapshot.RoadSnapshot north = snapshot.getRoad(Direction.NORTH);
        assertNotNull(north);
        assertEquals(SignalColor.GREEN, north.getSignalColor());
        assertEquals(1, north.getQueueLength());
        assertEquals(1, north.getVehicles().size());

        SimulationSnapshot.VehicleSnapshot car = north.getVehicles().get(0);
        assertEquals("CAR-N1", car.getId());
        assertEquals("Car", car.getType());
        assertTrue(car.isCar());
        assertFalse(car.isEmergency());

        SimulationSnapshot.RoadSnapshot south = snapshot.getRoad(Direction.SOUTH);
        assertNotNull(south);
        assertEquals(SignalColor.RED, south.getSignalColor());
        assertEquals(1, south.getQueueLength());

        SimulationSnapshot.VehicleSnapshot amb = south.getVehicles().get(0);
        assertEquals("AMB-S1", amb.getId());
        assertTrue(amb.isEmergency());
        assertTrue(amb.isAmbulance());
        assertEquals(5, amb.getUrgencyLevel());
    }

    @Test
    @DisplayName("Test 2: Advancing the simulation does not mutate previously captured snapshot")
    void testSnapshotIsFrozenAndUnaffectedByEngineTicks() {
        engine.addVehicle(Direction.NORTH, new Car("CAR-N1", 40, 2));

        SimulationSnapshot frozenSnapshot = engine.getSnapshot();
        assertEquals(0, frozenSnapshot.getTotalTicks());
        assertEquals(0, frozenSnapshot.getTotalVehiclesServed());
        assertEquals(1, frozenSnapshot.getRoad(Direction.NORTH).getQueueLength());

        // Advance simulation by 5 ticks
        engine.step(5);

        // Engine has progressed
        assertEquals(5, engine.getTotalTicks());
        assertEquals(1, engine.getTotalVehiclesServed());
        assertEquals(0, engine.getIntersection().getRoad(Direction.NORTH).getQueueLength());

        // Snapshot remains completely frozen at initial state
        assertEquals(0, frozenSnapshot.getTotalTicks(), "Snapshot tick counter must remain unchanged");
        assertEquals(0, frozenSnapshot.getTotalVehiclesServed(), "Snapshot vehicles served must remain unchanged");
        assertEquals(1, frozenSnapshot.getRoad(Direction.NORTH).getQueueLength(), "Snapshot queue must remain unchanged");
        assertEquals(1, frozenSnapshot.getRoad(Direction.NORTH).getVehicles().size());
    }

    @Test
    @DisplayName("Test 3: Snapshot collections are immutable and throw UnsupportedOperationException")
    void testSnapshotCollectionsAreImmutable() {
        engine.addVehicle(Direction.NORTH, new Car("CAR-N1", 40, 2));
        SimulationSnapshot snapshot = engine.getSnapshot();

        List<SimulationSnapshot.RoadSnapshot> roads = snapshot.getRoads();
        assertThrows(UnsupportedOperationException.class, () -> roads.remove(0),
                "Snapshot roads list must be unmodifiable");

        List<SimulationSnapshot.VehicleSnapshot> vehicles = snapshot.getRoad(Direction.NORTH).getVehicles();
        assertThrows(UnsupportedOperationException.class, () -> vehicles.clear(),
                "RoadSnapshot vehicles list must be unmodifiable");
    }

    @Test
    @DisplayName("Test 4: Safe vehicle injection validation and capacity bounds")
    void testSafeVehicleInjection() {
        // Valid injection by index
        assertTrue(engine.addVehicle(0, new Car("CAR-N1", 50, 4)));
        assertEquals(1, engine.getIntersection().getRoad(0).getQueueLength());

        // Valid injection by Direction
        assertTrue(engine.addVehicle(Direction.EAST, new Bike("BIKE-E1", 20, true)));
        assertEquals(1, engine.getIntersection().getRoad(Direction.EAST).getQueueLength());

        // Invalid arguments
        assertFalse(engine.addVehicle(-1, new Car("CAR-X", 40, 2)), "Invalid index should return false");
        assertFalse(engine.addVehicle(4, new Car("CAR-X", 40, 2)), "Out of bounds index should return false");
        assertFalse(engine.addVehicle((Direction) null, new Car("CAR-X", 40, 2)), "Null direction should return false");
        assertFalse(engine.addVehicle(0, null), "Null vehicle should return false");

        // Capacity bounds check
        Road north = engine.getIntersection().getRoad(Direction.NORTH);
        int remainingCapacity = north.getMaxCapacity() - north.getQueueLength();
        for (int i = 0; i < remainingCapacity; i++) {
            assertTrue(engine.addVehicle(Direction.NORTH, new Car("FILL-" + i, 30, 2)));
        }
        assertTrue(north.isFull());
        assertFalse(engine.addVehicle(Direction.NORTH, new Car("OVERFLOW", 30, 2)),
                "Adding vehicle to full road must return false");
    }

    @Test
    @DisplayName("Test 5: Concurrent vehicle injection while simulation controller is actively ticking")
    void testConcurrentVehicleInjectionWhileTicking() throws InterruptedException {
        SimulationController controller = new SimulationController(engine, 10);
        controller.start();

        AtomicBoolean injectionFailed = new AtomicBoolean(false);
        AtomicInteger successfulInjections = new AtomicInteger(0);

        // Inject vehicles concurrently from another thread
        Thread injectorThread = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                Vehicle v = (i % 4 == 0)
                        ? new Ambulance("CONC-AMB-" + i, 80, 5, "Trauma Center")
                        : new Car("CONC-CAR-" + i, 45, 2);

                Direction dir = Direction.values()[i % 4];
                boolean ok = engine.addVehicle(dir, v);
                if (ok) {
                    successfulInjections.incrementAndGet();
                }

                // Periodically capture snapshots concurrently
                SimulationSnapshot snapshot = engine.getSnapshot();
                if (snapshot == null || snapshot.getRoads().size() != 4) {
                    injectionFailed.set(true);
                }

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Test-VehicleInjector");

        injectorThread.start();
        injectorThread.join(3000);
        assertFalse(injectorThread.isAlive(), "Injector thread should complete cleanly");

        controller.stop();
        assertFalse(controller.isRunning());

        assertFalse(injectionFailed.get(), "No snapshot corruption should occur during concurrent execution");
        assertTrue(successfulInjections.get() > 0, "Vehicles should have been safely injected");
        assertTrue(engine.getTotalTicks() > 0, "Simulation should have advanced ticks");
    }

    @Test
    @DisplayName("Test 6: Safe retrieval of road snapshots by index and direction")
    void testRoadSnapshotLookup() {
        SimulationSnapshot snapshot = engine.getSnapshot();
        assertNotNull(snapshot.getRoad(0));
        assertNotNull(snapshot.getRoad(3));
        assertNull(snapshot.getRoad(-1));
        assertNull(snapshot.getRoad(4));
        assertNull(snapshot.getRoad((Direction) null));
    }
}

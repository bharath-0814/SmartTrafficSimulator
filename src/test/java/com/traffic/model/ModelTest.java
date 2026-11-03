package com.traffic.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 tests verifying the Phase 2 OOP Model classes with validations and encapsulation.
 */
class ModelTest {

    @Test
    @DisplayName("Test 1: Vehicle and Subclass Creation")
    void testVehicleCreation() {
        Car car = new Car("CAR-1", 50, 4);
        assertEquals("CAR-1", car.getId());
        assertEquals(50, car.getSpeed());
        assertEquals(4, car.getPassengers());
        assertEquals(0, car.getWaitingTime());
        assertFalse(car.isEmergency());
        assertEquals(1, car.calculatePriority());

        Bike bike = new Bike("BIKE-1", 30, true);
        assertEquals("BIKE-1", bike.getId());
        assertTrue(bike.hasHelmet());
        assertFalse(bike.isEmergency());

        Bus bus = new Bus("BUS-1", 40, 50);
        assertEquals("BUS-1", bus.getId());
        assertEquals(50, bus.getPassengerCapacity());
        assertEquals(2, bus.calculatePriority()); // Higher priority for public transit
    }

    @Test
    @DisplayName("Test 2: Multilevel Inheritance (Vehicle -> EmergencyVehicle -> Ambulance)")
    void testMultilevelInheritance() {
        Ambulance ambulance = new Ambulance("AMB-1", 80, 5, "Apollo Hospital");

        // Level 3 subclass is an instance of Level 2 parent
        assertTrue(ambulance instanceof EmergencyVehicle, "Ambulance must be an EmergencyVehicle");

        // Level 3 subclass is an instance of Level 1 grandparent
        assertTrue(ambulance instanceof Vehicle, "Ambulance must be a Vehicle");

        // Level 3 subclass implements EmergencyCapable interface
        assertTrue(ambulance instanceof EmergencyCapable, "Ambulance must implement EmergencyCapable");

        assertEquals("Apollo Hospital", ambulance.getHospitalName());
        assertEquals(5, ambulance.getUrgencyLevel());
        assertTrue(ambulance.isEmergency());
        assertEquals(10, ambulance.calculatePriority()); // Top priority for ambulance
    }

    @Test
    @DisplayName("Test 3: EmergencyCapable Interface Methods")
    void testEmergencyCapableInterface() {
        EmergencyCapable responder = new Ambulance("AMB-2", 75);

        assertTrue(responder.isSirenActive(), "Siren should be ON by default");
        responder.deactivateSiren();
        assertFalse(responder.isSirenActive(), "Siren should be OFF after deactivation");
        responder.activateSiren();
        assertTrue(responder.isSirenActive(), "Siren should be ON after reactivation");
        assertEquals(5, responder.getUrgencyLevel());
    }

    @Test
    @DisplayName("Test 4: Runtime Polymorphism with Heterogeneous Vehicle List")
    void testRuntimePolymorphism() {
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(new Car("C1", 45));
        vehicles.add(new Bike("B1", 25));
        vehicles.add(new Bus("BUS1", 35));
        vehicles.add(new Ambulance("AMB1", 85));

        int emergencyCount = 0;
        int totalPriority = 0;

        // Polymorphic loop: calling calculatePriority() on base Vehicle references
        for (Vehicle v : vehicles) {
            totalPriority += v.calculatePriority();
            if (v.isEmergency()) {
                emergencyCount++;
            }
        }

        assertEquals(1, emergencyCount, "Should identify exactly 1 emergency vehicle");
        // Car(1) + Bike(1) + Bus(2) + Ambulance(10) = 14
        assertEquals(14, totalPriority, "Sum of polymorphic priorities should equal 14");
    }

    @Test
    @DisplayName("Test 5: TrafficSignal State and Timer Ticking")
    void testTrafficSignal() {
        TrafficSignal signal = new TrafficSignal(10, 2);

        assertEquals(SignalColor.RED, signal.getColor(), "Signals should start at RED");
        assertTrue(signal.isRed());
        assertFalse(signal.isGreen());

        // Safe transition: RED -> GREEN
        signal.switchToGreen();
        assertTrue(signal.isGreen());
        assertEquals(10, signal.getTimer());

        signal.tick();
        assertEquals(9, signal.getTimer(), "Timer should count down after tick");

        // Safe transition: GREEN -> YELLOW
        signal.switchToYellow();
        assertTrue(signal.isYellow());
        assertEquals(2, signal.getTimer(), "Yellow light should use configured yellow duration");

        // Safe transition: YELLOW -> RED
        signal.switchToRed();
        assertTrue(signal.isRed());

        signal.setEmergencyOverride(true);
        assertTrue(signal.isEmergencyOverride());
    }

    @Test
    @DisplayName("Test 6: Road Queue Operations and Emergency Detection")
    void testRoadQueue() {
        Road northRoad = new Road("North Road", Direction.NORTH, 5);
        assertTrue(northRoad.isEmpty());
        assertFalse(northRoad.hasEmergencyVehicle());

        Car car1 = new Car("C1", 40);
        Car car2 = new Car("C2", 50);
        northRoad.addVehicle(car1);
        northRoad.addVehicle(car2);

        assertEquals(2, northRoad.getQueueLength());
        assertEquals(car1, northRoad.peekVehicle(), "Peek should return the front vehicle without removing");

        // Add an ambulance behind the two cars: queue is now [car1, car2, amb]
        Ambulance amb = new Ambulance("AMB-1", 70);
        northRoad.addVehicle(amb);
        assertTrue(northRoad.hasEmergencyVehicle(), "Road should now detect an emergency vehicle");

        // Emergency Vehicle Queue Priority: Ambulance must bypass ordinary cars and depart FIRST
        assertEquals(amb, northRoad.peekVehicle(), "Peek should return the emergency vehicle first");
        Vehicle departed1 = northRoad.removeVehicle();
        assertEquals(amb, departed1, "Emergency vehicle must depart first even when queued behind ordinary cars");

        // Normal commuter vehicles resume FIFO departure
        Vehicle departed2 = northRoad.removeVehicle();
        assertEquals(car1, departed2, "Ordinary vehicles depart in FIFO order after emergency vehicles clear");

        Vehicle departed3 = northRoad.removeVehicle();
        assertEquals(car2, departed3);

        assertTrue(northRoad.isEmpty());
        assertNull(northRoad.removeVehicle(), "Removing from empty road should return null");
    }

    @Test
    @DisplayName("Test 7: 4-Way Intersection Assembly")
    void testIntersection() {
        Intersection intersection = new Intersection();
        assertEquals(4, intersection.getRoads().length, "Intersection must have 4 roads");

        assertNotNull(intersection.getRoad(Direction.NORTH));
        assertNotNull(intersection.getRoad(Direction.EAST));
        assertNotNull(intersection.getRoad(Direction.SOUTH));
        assertNotNull(intersection.getRoad(Direction.WEST));

        assertFalse(intersection.hasEmergencyOnAnyRoad(), "Initially no emergency on any road");

        // Add ambulance to East road
        intersection.getRoad(Direction.EAST).addVehicle(new Ambulance("AMB-EAST", 80));

        assertTrue(intersection.hasEmergencyOnAnyRoad(), "Should detect emergency on East road");
        assertEquals(intersection.getRoad(Direction.EAST), intersection.getRoadWithEmergency());
    }

    @Test
    @DisplayName("Test 8: Basic Validation Rules with IllegalArgumentException")
    void testInputValidation() {
        // Speed cannot be negative
        assertThrows(IllegalArgumentException.class, () -> new Car("C1", -10, 2));

        // ID cannot be null or empty
        assertThrows(IllegalArgumentException.class, () -> new Car(null, 40, 2));
        assertThrows(IllegalArgumentException.class, () -> new Car("   ", 40, 2));

        // Passenger count must be positive
        assertThrows(IllegalArgumentException.class, () -> new Car("C2", 40, 0));
        assertThrows(IllegalArgumentException.class, () -> new Car("C3", 40, -1));

        // Bus capacity must be positive
        assertThrows(IllegalArgumentException.class, () -> new Bus("B1", 40, 0));
        assertThrows(IllegalArgumentException.class, () -> new Bus("B2", 40, -5));

        // Ambulance urgency must be between 1 and 5
        assertThrows(IllegalArgumentException.class, () -> new Ambulance("A1", 80, 0, "Hospital"));
        assertThrows(IllegalArgumentException.class, () -> new Ambulance("A2", 80, 6, "Hospital"));

        // Road capacity must be positive
        assertThrows(IllegalArgumentException.class, () -> new Road("North", Direction.NORTH, 0));
        assertThrows(IllegalArgumentException.class, () -> new Road("North", Direction.NORTH, -10));

        // Signal durations must be positive
        assertThrows(IllegalArgumentException.class, () -> new TrafficSignal(0));
        assertThrows(IllegalArgumentException.class, () -> new TrafficSignal(-5));
        assertThrows(IllegalArgumentException.class, () -> new TrafficSignal(10, 0));
    }

    @Test
    @DisplayName("Test 9: Encapsulation Integrity (Defensive Copies)")
    void testEncapsulationIntegrity() {
        Road road = new Road("East Road", Direction.EAST, 10);
        road.addVehicle(new Car("C1", 40, 2));
        assertEquals(1, road.getQueueLength());

        // Modifying the returned vehicles list should NOT modify road's internal queue
        ArrayList<Vehicle> externalList = road.getVehicles();
        externalList.clear(); // Clear external copy
        assertEquals(1, road.getQueueLength(), "Road internal queue must remain untouched after modifying external copy");

        // Safe index-based getter
        assertEquals("C1", road.getVehicle(0).getId());
        assertNull(road.getVehicle(99), "Out of bounds index returns null safely");

        // Modifying the returned roads array should NOT modify intersection's internal array
        Intersection intersection = new Intersection();
        Road[] roadsCopy = intersection.getRoads();
        roadsCopy[0] = null; // Clear first element in external copy
        assertNotNull(intersection.getRoad(0), "Intersection internal road array must remain intact");
    }
}

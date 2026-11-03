package com.traffic.model;

import java.util.ArrayList;

/**
 * Class representing one incoming road at the intersection.
 *
 * OOP Concepts:
 * 1. Composition ("has-a"): A Road HAS A TrafficSignal, and HAS A list of Vehicles.
 * 2. Encapsulation: The vehicle queue is protected, modified only via addVehicle() and removeVehicle().
 *    getVehicles() returns a copy to prevent external mutation of the internal queue.
 * 3. Polymorphism: The queue holds generic Vehicle references, so Cars, Bikes, Buses,
 *    and Ambulances can all be stored together in the same list.
 */
public class Road {

    public static final int DEFAULT_MAX_CAPACITY = 20;

    private String name;
    private Direction direction;
    private TrafficSignal signal;          // Composition: Road has a TrafficSignal
    private ArrayList<Vehicle> queue;      // Composition: Road has a list of Vehicles
    private int maxCapacity;

    /**
     * Parameterized Constructor with custom maximum capacity and validation.
     */
    public Road(String name, Direction direction, int maxCapacity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Road name cannot be null or empty.");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Road direction cannot be null.");
        }
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Road capacity must be positive: " + maxCapacity);
        }
        this.name = name.trim();
        this.direction = direction;
        this.maxCapacity = maxCapacity;
        this.signal = new TrafficSignal(); // Creates a default traffic light for this road
        this.queue = new ArrayList<>();    // Empty queue to start
    }

    /**
     * Overloaded Constructor: Uses default capacity (20 vehicles).
     */
    public Road(String name, Direction direction) {
        this(name, direction, DEFAULT_MAX_CAPACITY);
    }

    // Adds a vehicle to the back of the waiting queue (Thread-safe)
    public boolean addVehicle(Vehicle v) {
        if (v == null) {
            return false;
        }
        if (isFull()) {
            System.out.println("Cannot add " + v.getId() + ": " + name + " is at full capacity!");
            return false;
        }
        queue.add(v);
        return true;
    }

    /**
     * Removes and returns the next vehicle to depart from this road approach (Thread-safe).
     *
     * Priority Rule:
     * If an emergency vehicle (e.g. Ambulance) is anywhere in the queue, it is
     * granted immediate lane priority to bypass ordinary traffic. If multiple
     * emergency vehicles exist, the one with highest urgency departs first.
     * When no emergency vehicles are present, vehicles depart in normal FIFO order.
     */
    public Vehicle removeVehicle() {
        if (queue.isEmpty()) {
            return null;
        }

        // Search for emergency vehicle with the highest urgency
        int emergencyIndex = -1;
        int maxUrgency = 0;

        for (int i = 0; i < queue.size(); i++) {
            Vehicle v = queue.get(i);
            if (v instanceof EmergencyCapable) {
                EmergencyCapable ev = (EmergencyCapable) v;
                if (ev.getUrgencyLevel() > maxUrgency) {
                    maxUrgency = ev.getUrgencyLevel();
                    emergencyIndex = i;
                }
            } else if (v.isEmergency() && emergencyIndex == -1) {
                emergencyIndex = i;
            }
        }

        // Emergency vehicle departs first (bypassing normal traffic)
        if (emergencyIndex != -1) {
            return queue.remove(emergencyIndex);
        }

        // Normal commuter traffic departs in FIFO order
        return queue.remove(0);
    }

    /**
     * Peeks at the next vehicle scheduled to depart without removing it (Thread-safe).
     */
    public Vehicle peekVehicle() {
        if (queue.isEmpty()) {
            return null;
        }

        int emergencyIndex = -1;
        int maxUrgency = 0;

        for (int i = 0; i < queue.size(); i++) {
            Vehicle v = queue.get(i);
            if (v instanceof EmergencyCapable) {
                EmergencyCapable ev = (EmergencyCapable) v;
                if (ev.getUrgencyLevel() > maxUrgency) {
                    maxUrgency = ev.getUrgencyLevel();
                    emergencyIndex = i;
                }
            } else if (v.isEmergency() && emergencyIndex == -1) {
                emergencyIndex = i;
            }
        }

        if (emergencyIndex != -1) {
            return queue.get(emergencyIndex);
        }

        return queue.get(0);
    }

    // Safely retrieves a vehicle at a specific queue position without removing it (Thread-safe)
    public Vehicle getVehicle(int index) {
        if (index >= 0 && index < queue.size()) {
            return queue.get(index);
        }
        return null;
    }

    // Checks if any vehicle currently waiting in line is an emergency vehicle (Thread-safe)
    public boolean hasEmergencyVehicle() {
        for (Vehicle v : queue) {
            if (v.isEmergency()) {
                return true; // Found an ambulance!
            }
        }
        return false;
    }

    public int getQueueLength() {
        return queue.size();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public boolean isFull() {
        return queue.size() >= maxCapacity;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    public TrafficSignal getSignal() {
        return signal;
    }

    /**
     * Returns a copy of the vehicle queue to protect internal state (Encapsulation & Thread-safe).
     * Outside code modifying the returned list will NOT affect the road's actual queue.
     */
    public ArrayList<Vehicle> getVehicles() {
        return new ArrayList<>(queue);
    }

    @Override
    public String toString() {
        return name + " (" + direction + ") [Queue: " + queue.size() + "/" + maxCapacity + ", Light: " + signal.getColor() + "]";
    }
}


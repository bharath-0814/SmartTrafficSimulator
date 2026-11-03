package com.traffic.model;

/**
 * Class representing the entire four-way intersection.
 *
 * OOP Concepts:
 * 1. Composition: An Intersection HAS-A fixed array of 4 Roads (North, East, South, West).
 * 2. Arrays: Uses a single-dimensional array Road[4] to store approaches.
 * 3. Encapsulation: Protects internal array state by returning defensive copies and safe access methods.
 */
public class Intersection {

    // Fixed array of 4 approaches:
    // Index 0 = NORTH, Index 1 = EAST, Index 2 = SOUTH, Index 3 = WEST
    private Road[] roads;

    /**
     * Default Constructor: Sets up the 4 approaches of the intersection.
     */
    public Intersection() {
        this.roads = new Road[4];
        this.roads[0] = new Road("North Approach", Direction.NORTH);
        this.roads[1] = new Road("East Approach", Direction.EAST);
        this.roads[2] = new Road("South Approach", Direction.SOUTH);
        this.roads[3] = new Road("West Approach", Direction.WEST);
    }

    /**
     * Returns a copy of the roads array so external callers cannot overwrite
     * internal array references (Encapsulation).
     */
    public Road[] getRoads() {
        return new Road[] { roads[0], roads[1], roads[2], roads[3] };
    }

    public int getRoadCount() {
        return roads.length;
    }

    public Road getRoad(int index) {
        if (index >= 0 && index < roads.length) {
            return roads[index];
        }
        return null;
    }

    // Helper to find a road by its cardinal direction
    public Road getRoad(Direction direction) {
        if (direction == null) {
            return null;
        }
        for (Road road : roads) {
            if (road.getDirection() == direction) {
                return road;
            }
        }
        return null;
    }

    // Checks if ANY of the 4 roads has an ambulance waiting
    public boolean hasEmergencyOnAnyRoad() {
        for (Road road : roads) {
            if (road.hasEmergencyVehicle()) {
                return true;
            }
        }
        return false;
    }

    // Returns the road that currently has an ambulance waiting (or null if none)
    public Road getRoadWithEmergency() {
        for (Road road : roads) {
            if (road.hasEmergencyVehicle()) {
                return road;
            }
        }
        return null;
    }
}

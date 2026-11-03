package com.traffic.model;

/**
 * Abstract base class representing any vehicle in the simulation.
 *
 * OOP Concepts:
 * 1. Abstraction (abstract class): We cannot instantiate a generic "Vehicle" directly;
 *    it acts as a common blueprint for Car, Bike, Bus, and Ambulance.
 * 2. Encapsulation: Fields are private, and access is controlled through public getters/setters.
 */
public abstract class Vehicle {

    // Private variables: Hidden from direct outside access (Encapsulation)
    private String id;
    private int speed;
    private int waitingTime; // Counts how many seconds/ticks this vehicle has waited

    /**
     * Parameterized Constructor: Initializes common vehicle properties with basic validation.
     */
    public Vehicle(String id, int speed) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle ID cannot be null or empty.");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Vehicle speed cannot be negative: " + speed);
        }
        this.id = id.trim();
        this.speed = speed;
        this.waitingTime = 0; // Waiting time starts at 0
    }

    // --- Getters and Setters (Encapsulation) ---

    public String getId() {
        return id;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        if (speed < 0) {
            throw new IllegalArgumentException("Vehicle speed cannot be negative: " + speed);
        }
        this.speed = speed;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        if (waitingTime < 0) {
            throw new IllegalArgumentException("Waiting time cannot be negative: " + waitingTime);
        }
        this.waitingTime = waitingTime;
    }

    // Increments the vehicle's waiting counter by 1 second
    public void incrementWaitingTime() {
        this.waitingTime++;
    }

    /**
     * Checks if this vehicle is an emergency responder.
     * Default for regular vehicles is false; overridden by emergency vehicles.
     */
    public boolean isEmergency() {
        return false;
    }

    // --- Abstract Methods: Must be implemented by child classes (Polymorphism) ---

    // Prints or displays vehicle-specific information
    public abstract void displayInfo();

    // Returns a priority rating for traffic scheduling (higher number = higher priority)
    public abstract int calculatePriority();

    @Override
    public String toString() {
        return getId() + " (" + getClass().getSimpleName() + ")";
    }
}

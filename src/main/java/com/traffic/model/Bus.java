package com.traffic.model;

/**
 * Concrete class representing a public transit bus with passenger capacity.
 *
 * OOP Concepts:
 * 1. Inheritance: Extends Vehicle to represent mass transit.
 * 2. super keyword: Passes common data to the Vehicle superclass.
 * 3. Method Overriding: Overrides displayInfo() and calculatePriority().
 */
public class Bus extends Vehicle {

    private int passengerCapacity; // Specific to Bus

    public Bus(String id, int speed, int passengerCapacity) {
        super(id, speed);
        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException("Passenger capacity must be positive: " + passengerCapacity);
        }
        this.passengerCapacity = passengerCapacity;
    }

    public Bus(String id, int speed) {
        this(id, speed, 40); // Default capacity = 40
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(int passengerCapacity) {
        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException("Passenger capacity must be positive: " + passengerCapacity);
        }
        this.passengerCapacity = passengerCapacity;
    }

    @Override
    public void displayInfo() {
        System.out.println("Bus [" + getId() + "] Speed: " + getSpeed() + " km/h, Capacity: " + passengerCapacity);
    }

    @Override
    public int calculatePriority() {
        // Public buses get priority = 2 (higher than normal cars because they carry many people)
        return 2;
    }
}

package com.traffic.model;

/**
 * Concrete class representing a standard passenger automobile.
 *
 * OOP Concepts:
 * 1. Inheritance (extends Vehicle): Reuses code from the parent Vehicle class.
 * 2. super keyword: Calls the parent Vehicle constructor using super(id, speed).
 * 3. Method Overriding (@Override): Provides its own version of displayInfo() and calculatePriority().
 * 4. Constructor Overloading: Provides two different constructors with different parameter lists.
 */
public class Car extends Vehicle {

    private int passengers; // Specific to Car

    /**
     * Parameterized Constructor with passenger count.
     */
    public Car(String id, int speed, int passengers) {
        super(id, speed); // Passes common data to the Vehicle parent constructor
        if (passengers <= 0) {
            throw new IllegalArgumentException("Passenger count must be positive: " + passengers);
        }
        this.passengers = passengers;
    }

    /**
     * Overloaded Constructor: Defaults passenger count to 1.
     */
    public Car(String id, int speed) {
        this(id, speed, 1); // Calls the other constructor in this class (constructor chaining)
    }

    public int getPassengers() {
        return passengers;
    }

    public void setPassengers(int passengers) {
        if (passengers <= 0) {
            throw new IllegalArgumentException("Passenger count must be positive: " + passengers);
        }
        this.passengers = passengers;
    }

    @Override
    public void displayInfo() {
        System.out.println("Car [" + getId() + "] Speed: " + getSpeed() + " km/h, Passengers: " + passengers);
    }

    @Override
    public int calculatePriority() {
        // Standard commuter car has normal base priority = 1
        return 1;
    }
}

package com.traffic.model;

/**
 * Concrete class representing a two-wheeled motorcycle or bicycle.
 *
 * OOP Concepts:
 * 1. Inheritance: Extends the base Vehicle class.
 * 2. super keyword: Delegates initialization to the parent constructor.
 * 3. Method Overriding: Overrides displayInfo() and calculatePriority().
 */
public class Bike extends Vehicle {

    private boolean hasHelmet; // Specific to Bike

    public Bike(String id, int speed, boolean hasHelmet) {
        super(id, speed);
        this.hasHelmet = hasHelmet;
    }

    public Bike(String id, int speed) {
        this(id, speed, true); // Default has helmet = true
    }

    public boolean hasHelmet() {
        return hasHelmet;
    }

    public void setHasHelmet(boolean hasHelmet) {
        this.hasHelmet = hasHelmet;
    }

    @Override
    public void displayInfo() {
        System.out.println("Bike [" + getId() + "] Speed: " + getSpeed() + " km/h, Helmet: " + hasHelmet);
    }

    @Override
    public int calculatePriority() {
        // Standard two-wheeler has base priority = 1
        return 1;
    }
}

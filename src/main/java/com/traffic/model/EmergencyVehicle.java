package com.traffic.model;

/**
 * Abstract class representing any emergency vehicle.
 *
 * OOP Concepts:
 * 1. Multilevel Inheritance (Level 2):
 *    Vehicle (Parent) -> EmergencyVehicle (Child) -> Ambulance (Grandchild).
 * 2. Interface Implementation: Implements EmergencyCapable to handle sirens and urgency.
 * 3. Method Overriding: Overrides isEmergency() to return true.
 */
public abstract class EmergencyVehicle extends Vehicle implements EmergencyCapable {

    private boolean sirenActive;
    private int urgencyLevel; // Rating from 1 (low) to 5 (critical emergency)

    /**
     * Parameterized Constructor: Initializes vehicle base properties and emergency state with validation.
     */
    public EmergencyVehicle(String id, int speed, int urgencyLevel) {
        super(id, speed); // Calls the Vehicle parent constructor
        if (urgencyLevel < 1 || urgencyLevel > 5) {
            throw new IllegalArgumentException("Urgency level must be between 1 and 5: " + urgencyLevel);
        }
        this.urgencyLevel = urgencyLevel;
        this.sirenActive = true; // Emergency sirens are active by default
    }

    // --- EmergencyCapable Interface Methods ---

    @Override
    public void activateSiren() {
        this.sirenActive = true;
    }

    @Override
    public void deactivateSiren() {
        this.sirenActive = false;
    }

    @Override
    public boolean isSirenActive() {
        return sirenActive;
    }

    @Override
    public int getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(int urgencyLevel) {
        if (urgencyLevel < 1 || urgencyLevel > 5) {
            throw new IllegalArgumentException("Urgency level must be between 1 and 5: " + urgencyLevel);
        }
        this.urgencyLevel = urgencyLevel;
    }

    // Overrides Vehicle.isEmergency() to return true for all emergency vehicles
    @Override
    public boolean isEmergency() {
        return true;
    }
}

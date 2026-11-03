package com.traffic.model;

/**
 * Concrete class representing an emergency medical ambulance.
 *
 * OOP Concepts:
 * 1. Multilevel Inheritance (Level 3):
 *    Vehicle -> EmergencyVehicle -> Ambulance.
 * 2. Method Overriding: Overrides displayInfo() and calculatePriority().
 * 3. Constructor Overloading: Multiple constructors for convenience.
 */
public class Ambulance extends EmergencyVehicle {

    private String hospitalName;

    /**
     * Parameterized Constructor with all details.
     */
    public Ambulance(String id, int speed, int urgencyLevel, String hospitalName) {
        super(id, speed, urgencyLevel); // Calls EmergencyVehicle constructor
        this.hospitalName = hospitalName;
    }

    /**
     * Overloaded Constructor: Default critical urgency (5) and default hospital.
     */
    public Ambulance(String id, int speed) {
        this(id, speed, 5, "City General Hospital");
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    @Override
    public void displayInfo() {
        System.out.println("AMBULANCE [" + getId() + "] Speed: " + getSpeed() + " km/h"
                + ", Urgency: " + getUrgencyLevel() + "/5"
                + ", Siren: " + (isSirenActive() ? "ON" : "OFF")
                + ", Hospital: " + hospitalName);
    }

    @Override
    public int calculatePriority() {
        // High priority score (10) ensures ambulances trigger green signal overrides
        return 10;
    }
}

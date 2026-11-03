package com.traffic.model;

/**
 * Interface representing emergency capabilities.
 *
 * OOP Concept: Interface Abstraction
 * - An interface defines WHAT a class can do, without specifying HOW.
 * - Classes that implement this interface (like EmergencyVehicle and Ambulance)
 *   must provide real code for each of these methods.
 */
public interface EmergencyCapable {

    // Turns on the emergency siren and flashing lights
    void activateSiren();

    // Turns off the emergency siren
    void deactivateSiren();

    // Checks if the siren is currently on
    boolean isSirenActive();

    // Returns the urgency level of the emergency (e.g., 1 to 5)
    int getUrgencyLevel();
}

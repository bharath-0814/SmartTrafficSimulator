package com.traffic.model;

/**
 * Enumeration representing the optical color of a traffic light.
 *
 * OOP Concept: Type-Safe Enumeration (enum)
 * - Restricts the signal state to only valid values: RED, YELLOW, or GREEN.
 * - Prevents typo bugs like "red" vs "RED".
 */
public enum SignalColor {
    RED,
    YELLOW,
    GREEN
}

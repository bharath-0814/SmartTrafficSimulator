package com.traffic.model;

/**
 * Cardinal directions for the four roads approaching the intersection.
 *
 * OOP Concept: Enum with custom methods and string representations.
 */
public enum Direction {
    NORTH("North"),
    EAST("East"),
    SOUTH("South"),
    WEST("West");

    private final String displayName;

    Direction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

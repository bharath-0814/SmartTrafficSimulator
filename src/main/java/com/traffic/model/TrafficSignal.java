package com.traffic.model;

/**
 * Class representing a traffic light controlling a single road approach.
 *
 * OOP Concepts:
 * 1. Encapsulation: Signal color and timers are private, manipulated through controlled methods.
 * 2. Constructor Overloading: Provides default and customized timing constructors.
 */
public class TrafficSignal {

    public static final int DEFAULT_GREEN_DURATION = 10; // Default 10 seconds green
    public static final int DEFAULT_YELLOW_DURATION = 2; // Default 2 seconds yellow

    private SignalColor color;
    private int greenDuration;
    private int yellowDuration;
    private int timer;                  // Remaining seconds for current phase
    private boolean emergencyOverride;  // True if forced green for an ambulance

    /**
     * Parameterized Constructor with custom green and yellow durations.
     */
    public TrafficSignal(int greenDuration, int yellowDuration) {
        if (greenDuration <= 0) {
            throw new IllegalArgumentException("Green duration must be positive: " + greenDuration);
        }
        if (yellowDuration <= 0) {
            throw new IllegalArgumentException("Yellow duration must be positive: " + yellowDuration);
        }
        this.color = SignalColor.RED; // Signals start at RED by default
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.timer = 0;
        this.emergencyOverride = false;
    }

    /**
     * Overloaded Constructor with custom green duration and default yellow.
     */
    public TrafficSignal(int greenDuration) {
        this(greenDuration, DEFAULT_YELLOW_DURATION);
    }

    /**
     * Overloaded Constructor: Uses default green and yellow durations.
     */
    public TrafficSignal() {
        this(DEFAULT_GREEN_DURATION, DEFAULT_YELLOW_DURATION);
    }

    // --- State and Timer Methods (Thread-safe) ---

    public synchronized SignalColor getColor() {
        return color;
    }

    /**
     * Changes signal color and configures the countdown timer appropriately (Thread-safe).
     * Sequence: RED -> GREEN -> YELLOW -> RED
     */
    public synchronized void changeColor(SignalColor newColor) {
        if (newColor == null) {
            throw new IllegalArgumentException("Signal color cannot be null.");
        }
        this.color = newColor;
        if (newColor == SignalColor.GREEN) {
            this.timer = greenDuration;
        } else if (newColor == SignalColor.YELLOW) {
            this.timer = yellowDuration;
        } else {
            this.timer = 0;
        }
    }

    // Convenient transition helpers for simulation (Thread-safe)
    public synchronized void switchToGreen() {
        changeColor(SignalColor.GREEN);
    }

    public synchronized void switchToYellow() {
        changeColor(SignalColor.YELLOW);
    }

    public synchronized void switchToRed() {
        changeColor(SignalColor.RED);
    }

    public synchronized boolean isGreen() {
        return this.color == SignalColor.GREEN;
    }

    public synchronized boolean isRed() {
        return this.color == SignalColor.RED;
    }

    public synchronized boolean isYellow() {
        return this.color == SignalColor.YELLOW;
    }

    // Advances the countdown timer by 1 second (Thread-safe)
    public synchronized void tick() {
        if (timer > 0) {
            timer--;
        }
    }

    public synchronized boolean isTimerExpired() {
        return timer <= 0;
    }

    public synchronized int getTimer() {
        return timer;
    }

    public synchronized int getGreenDuration() {
        return greenDuration;
    }

    public synchronized void setGreenDuration(int greenDuration) {
        if (greenDuration <= 0) {
            throw new IllegalArgumentException("Green duration must be positive: " + greenDuration);
        }
        this.greenDuration = greenDuration;
    }

    public synchronized int getYellowDuration() {
        return yellowDuration;
    }

    public synchronized void setYellowDuration(int yellowDuration) {
        if (yellowDuration <= 0) {
            throw new IllegalArgumentException("Yellow duration must be positive: " + yellowDuration);
        }
        this.yellowDuration = yellowDuration;
    }

    // --- Emergency Override Controls (Thread-safe) ---

    public synchronized boolean isEmergencyOverride() {
        return emergencyOverride;
    }

    public synchronized void setEmergencyOverride(boolean emergencyOverride) {
        this.emergencyOverride = emergencyOverride;
    }

    @Override
    public String toString() {
        return "TrafficSignal[" + color + ", timer=" + timer + "s"
                + (emergencyOverride ? ", EMERGENCY_OVERRIDE" : "") + "]";
    }
}

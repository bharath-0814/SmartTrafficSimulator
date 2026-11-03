package com.traffic.engine;

import com.traffic.model.Direction;
import com.traffic.model.SignalColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable point-in-time snapshot of the traffic simulation state.
 *
 * <p>Why we need this class:
 * In a multithreaded simulation, the background worker thread constantly updates
 * vehicle positions, traffic light timers, and queues. If the Swing GUI reads these
 * live objects directly, it could read values from different simulation ticks (tearing)
 * or attempt to render a vehicle while the engine is modifying it.
 *
 * <p>To prevent race conditions, the engine creates this read-only snapshot inside
 * a single synchronized block. The GUI then safely renders from this frozen copy
 * without touching any live model objects.
 *
 * <p>OOP Concepts Demonstrated:
 * <ul>
 *   <li><b>Encapsulation:</b> All fields are private and final; data cannot be altered after creation.</li>
 *   <li><b>Immutability:</b> All list getters return unmodifiable copies so callers cannot mutate state.</li>
 * </ul>
 */
public class SimulationSnapshot {

    private final int totalTicks;
    private final int totalVehiclesServed;
    private final int totalEmergencyVehiclesServed;
    private final int currentGreenIndex;
    private final int greenDuration;
    private final int yellowDuration;
    private final int starvationThreshold;
    private final boolean emergencyActive;
    private final String lastEventDescription;
    private final List<RoadSnapshot> roads;

    public SimulationSnapshot(int totalTicks,
                              int totalVehiclesServed,
                              int totalEmergencyVehiclesServed,
                              int currentGreenIndex,
                              int greenDuration,
                              int yellowDuration,
                              int starvationThreshold,
                              boolean emergencyActive,
                              String lastEventDescription,
                              List<RoadSnapshot> roads) {
        this.totalTicks = totalTicks;
        this.totalVehiclesServed = totalVehiclesServed;
        this.totalEmergencyVehiclesServed = totalEmergencyVehiclesServed;
        this.currentGreenIndex = currentGreenIndex;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.starvationThreshold = starvationThreshold;
        this.emergencyActive = emergencyActive;
        this.lastEventDescription = (lastEventDescription != null) ? lastEventDescription : "";
        this.roads = (roads != null) ? Collections.unmodifiableList(new ArrayList<>(roads)) : Collections.emptyList();
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getTotalVehiclesServed() {
        return totalVehiclesServed;
    }

    public int getTotalEmergencyVehiclesServed() {
        return totalEmergencyVehiclesServed;
    }

    public int getCurrentGreenIndex() {
        return currentGreenIndex;
    }

    public int getGreenDuration() {
        return greenDuration;
    }

    public int getYellowDuration() {
        return yellowDuration;
    }

    public int getStarvationThreshold() {
        return starvationThreshold;
    }

    public boolean isEmergencyActive() {
        return emergencyActive;
    }

    public String getLastEventDescription() {
        return lastEventDescription;
    }

    public List<RoadSnapshot> getRoads() {
        return roads;
    }

    public RoadSnapshot getRoad(int index) {
        if (index >= 0 && index < roads.size()) {
            return roads.get(index);
        }
        return null;
    }

    public RoadSnapshot getRoad(Direction direction) {
        for (RoadSnapshot r : roads) {
            if (r.getDirection() == direction) {
                return r;
            }
        }
        return null;
    }

    public RoadSnapshot getActiveRoad() {
        return getRoad(currentGreenIndex);
    }

    // =========================================================================
    // Nested Immutable Road Snapshot
    // =========================================================================

    /**
     * Immutable snapshot of a single road approach.
     */
    public static class RoadSnapshot {
        private final String name;
        private final Direction direction;
        private final SignalColor signalColor;
        private final int signalTimer;
        private final boolean emergencyOverride;
        private final int maxCapacity;
        private final int queueLength;
        private final List<VehicleSnapshot> vehicles;

        public RoadSnapshot(String name,
                            Direction direction,
                            SignalColor signalColor,
                            int signalTimer,
                            boolean emergencyOverride,
                            int maxCapacity,
                            int queueLength,
                            List<VehicleSnapshot> vehicles) {
            this.name = (name != null) ? name : "";
            this.direction = direction;
            this.signalColor = (signalColor != null) ? signalColor : SignalColor.RED;
            this.signalTimer = signalTimer;
            this.emergencyOverride = emergencyOverride;
            this.maxCapacity = maxCapacity;
            this.queueLength = queueLength;
            this.vehicles = (vehicles != null) ? Collections.unmodifiableList(new ArrayList<>(vehicles)) : Collections.emptyList();
        }

        public String getName() {
            return name;
        }

        public Direction getDirection() {
            return direction;
        }

        public SignalColor getSignalColor() {
            return signalColor;
        }

        public int getSignalTimer() {
            return signalTimer;
        }

        public boolean isEmergencyOverride() {
            return emergencyOverride;
        }

        public boolean isGreen() {
            return signalColor == SignalColor.GREEN;
        }

        public boolean isYellow() {
            return signalColor == SignalColor.YELLOW;
        }

        public boolean isRed() {
            return signalColor == SignalColor.RED;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }

        public int getQueueLength() {
            return queueLength;
        }

        public boolean isFull() {
            return queueLength >= maxCapacity;
        }

        public boolean isEmpty() {
            return queueLength == 0;
        }

        public List<VehicleSnapshot> getVehicles() {
            return vehicles;
        }
    }

    // =========================================================================
    // Nested Immutable Vehicle Snapshot
    // =========================================================================

    /**
     * Immutable snapshot of an individual vehicle waiting in a queue.
     */
    public static class VehicleSnapshot {
        private final String id;
        private final String type;
        private final int speed;
        private final int waitingTime;
        private final boolean emergency;
        private final boolean ambulance;
        private final boolean bus;
        private final boolean bike;
        private final boolean car;
        private final int urgencyLevel;

        public VehicleSnapshot(String id,
                               String type,
                               int speed,
                               int waitingTime,
                               boolean emergency,
                               boolean ambulance,
                               boolean bus,
                               boolean bike,
                               boolean car,
                               int urgencyLevel) {
            this.id = (id != null) ? id : "";
            this.type = (type != null) ? type : "";
            this.speed = speed;
            this.waitingTime = waitingTime;
            this.emergency = emergency;
            this.ambulance = ambulance;
            this.bus = bus;
            this.bike = bike;
            this.car = car;
            this.urgencyLevel = urgencyLevel;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public int getSpeed() {
            return speed;
        }

        public int getWaitingTime() {
            return waitingTime;
        }

        public boolean isEmergency() {
            return emergency;
        }

        public boolean isAmbulance() {
            return ambulance;
        }

        public boolean isBus() {
            return bus;
        }

        public boolean isBike() {
            return bike;
        }

        public boolean isCar() {
            return car;
        }

        public int getUrgencyLevel() {
            return urgencyLevel;
        }
    }
}

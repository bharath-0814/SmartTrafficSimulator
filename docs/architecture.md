# System Architecture Document

## 1. Project Overview & Philosophy

The **Smart Traffic Management and Emergency Vehicle Prioritization Simulator** is designed as an educational, beginner-friendly Java application for university students studying **CSC2104: Object Oriented Programming**.

Unlike complex industrial simulations with complicated coordinate geometry or micro-routing, this system models **one single four-way intersection** with simple, deterministic rules that are clean to code, easy to debug, and straightforward to explain during a university viva voce examination.

> **Current Implementation Status:**  
> • **Phase 1 (Foundation):** COMPLETED  
> • **Phase 2 (Core OOP Models):** COMPLETED — All domain classes (`Vehicle`, `Car`, `Bike`, `Bus`, `EmergencyVehicle`, `Ambulance`, `EmergencyCapable`, `TrafficSignal`, `Road`, `Intersection`) are fully implemented and verified.  
> • **Phase 3 (Simulation Logic):** COMPLETED — `SimulationEngine` console state machine, preemption, and starvation protection verified.  
> • **Phase 4 (File Handling & Exceptions):** COMPLETED — `ReportService` and `ReportException` verified.  
> • **Phase 5 (Multithreading):** COMPLETED — `SimulationController` (`Runnable`, background `Thread`, `wait()`/`notifyAll()` pause/resume, `join()`, `volatile` flags) verified.  
> • **Phase 6 (GUI Visualizer & Snapshots):** COMPLETED — `MainFrame`, `IntersectionPanel`, and immutable `SimulationSnapshot` verified with 52 automated passing tests.

---

## 2. The 4-Way Intersection Model

```
                         NORTH ROAD
                     [Incoming Queue]
                            |
                            v [Traffic Light]
                            |
   WEST ROAD                +                EAST ROAD
[Incoming Queue] ---> [INTERSECTION BOX] <--- [Incoming Queue]
   [Traffic Light]          +                [Traffic Light]
                            ^
                            | [Traffic Light]
                            |
                         SOUTH ROAD
                     [Incoming Queue]
```

### Physical Components:
1. **Four Approaching Roads:** North, East, South, and West (`Road.java`).
2. **Four Traffic Signals:** One signal controlling entry for each approach (`TrafficSignal.java`).
3. **Traffic Queues:** Vehicles queue up at red lights and depart one-by-one when green (`ArrayList<Vehicle>`).
4. **Emergency Priority Corridor:** When an emergency vehicle (`Ambulance`) arrives on any road, the system prioritizes it by turning that road GREEN while holding all other approaches RED.

---

## 3. Simplified Layered Architecture & Data Flow

The system strictly enforces a unidirectional data flow to prevent race conditions and guarantee thread safety:

```
Main (Entry Point)
  │
  ├──> Model (Domain Entities: Vehicle, Road, TrafficSignal, Intersection)
  │      │
  │      ▼
  └──> SimulationEngine (State machine, Tick loop, Emergency rules)
         │
         ├──> SimulationController (Background Thread, Runnable, Start/Pause/Resume/Stop)
         │
         ├──> SimulationSnapshot (Immutable freeze-frame of all metrics, roads, signals, queues)
         │      │
         │      ├──> Swing GUI (MainFrame & IntersectionPanel 2D Canvas)
         │      │
         │      └──> ReportService (FileWriter, FileReader, BufferedReader, ReportException)
```

### Layer Breakdown:

1. **`com.traffic.model` (Domain Objects)**
   * Plain Java classes holding data, encapsulation, and basic operations.
   * `Vehicle` (abstract base class), `Car`, `Bike`, `Bus`, `EmergencyVehicle`, and `Ambulance`.
   * `EmergencyCapable` interface for sirens and urgency ratings.
   * `Road` (holds an `ArrayList<Vehicle>` queue, defensive copying, lane-level emergency queue priority, capacity limits, and synchronized methods).
   * `TrafficSignal` (manages `RED`, `YELLOW`, `GREEN` states, green/yellow durations, and emergency override).
   * `Intersection` (aggregates the 4 roads: North, East, South, West).
   * **Rule:** Zero UI code; zero Swing imports.

2. **`com.traffic.engine` (Simulation Logic, Threading & Snapshots)**
   * `SimulationEngine`: Drives the discrete simulation loop (`tick()` and `step()`).
   * Manages round-robin rotation (North &rarr; East &rarr; South &rarr; West), enforces single-green safety invariant, and handles `GREEN -> YELLOW -> RED` phase transitions.
   * Executes safe emergency preemption without abrupt cuts, prioritizes highest-urgency ambulances, and enforces anti-starvation thresholds for normal commuter traffic.
   * Increments waiting times strictly for vehicles stopped at RED signals.
   * `addVehicle(int roadIndex, Vehicle v)` and `addVehicle(Direction dir, Vehicle v)`: Synchronized methods allowing runtime vehicle injection without thread collision.
   * `getSnapshot()`: Captures an atomic, read-only freeze-frame of the entire intersection state.
   * `SimulationSnapshot`: Immutable snapshot data transfer object containing `RoadSnapshot` and `VehicleSnapshot` nested records.
   * `SimulationController`: Implements `Runnable` to run the simulation on a dedicated background `Thread`. Provides thread-safe `start()`, `pause()`, `resume()`, and `stop()` operations via `wait()` and `notifyAll()` with zero CPU consumption during pause, and controls tick pacing with `Thread.sleep(tickDelayMs)`.

3. **`com.traffic.service` (Utility & File Services)**
   * `ReportService`: Handles file writing (`FileWriter`) and reading (`FileReader`, `BufferedReader`) to format and save simulation summaries to `traffic_report.txt`.
   * Formats reports directly from immutable `SimulationSnapshot` instances, preventing inconsistencies while the engine ticks in the background.
   * `ReportException`: Custom checked exception extending `java.lang.Exception` wrapping low-level I/O errors.
   * Guarantees safe stream closure via `finally` blocks.

4. **`com.traffic.ui` (User Interface)**
   * `MainFrame`: The primary Swing dashboard window with dark styling, real-time control buttons (Start, Pause, Resume, Stop, Step Once), simulation speed slider, interactive vehicle injector, live metrics counters, and report generation/viewer dialogs.
   * `IntersectionPanel`: Custom 2D painting (`paintComponent`) rendering the top-down 4-way crossroads, yellow center dividers, stop lines, traffic signal housings with glowing bulbs/timers, and color-coded vehicles (including ambulances with a distinct red emergency cross).
   * Refreshed strictly on the Swing Event Dispatch Thread (EDT) at 20 Hz (50ms interval) via `javax.swing.Timer`.
   * **Thread-Safety Rule:** The GUI never queries mutable live engine models directly during repainting; it paints exclusively from the latest immutable `SimulationSnapshot`.

5. **`com.traffic.app` (Application Entry Point)**
   * `Main.java`: Contains `public static void main(String[] args)`, executes the multi-act console demonstration by default, and supports launching the Swing GUI with the `--gui` flag.

---

## 4. Multithreading & Snapshot Architecture

A common pitfall in beginner Java GUI projects is running long simulation loops directly on the Swing Event Dispatch Thread (EDT), which freezes the user interface, or having the GUI read live mutable domain models concurrently with a worker thread, causing race conditions and visual tearing.

Our design solves both problems cleanly:

```
+--------------------------------------------------------------------------------+
|                        Swing Event Dispatch Thread (EDT)                       |
|                                                                                |
|  [javax.swing.Timer @ 20Hz]                                                    |
|           │                                                                    |
|           ▼                                                                    |
|  1. Calls engine.getSnapshot()  <────────────── synchronized (engine)          |
|  2. Passes snapshot to IntersectionPanel                                       |
|  3. Repaints 2D Canvas from frozen snapshot                                    |
|  4. Updates UI Labels and Buttons                                              |
+--------------------------------------------------------------------------------+
                                       ▲
                                       │ (Atomic Snapshot)
+--------------------------------------------------------------------------------+
|                        Background Simulation Worker Thread                     |
|                   (SimulationController implements Runnable)                   |
|                                                                                |
|  while (running) {                                                             |
|      check pauseLock.wait()                                                    |
|      engine.tick()              <────────────── synchronized (engine)          |
|      Thread.sleep(tickDelayMs)                                                 |
|  }                                                                             |
+--------------------------------------------------------------------------------+
```

1. **Main / Caller Thread (Swing Event Dispatch Thread):**
   * Manages user commands (`start()`, `pause()`, `resume()`, `stop()`).
   * Listens to button clicks and user interactions.
   * Repaints the canvas using a `javax.swing.Timer` callback at 20 Hz (50ms).
2. **Simulation Background Worker Thread (`SimulationController implements Runnable`):**
   * Created as a dedicated thread: `new Thread(this, "TrafficSimulation-WorkerThread")`.
   * Executes the `run()` loop: calls `engine.tick()`, sleeps via `Thread.sleep(tickDelayMs)`.
3. **Immutable Snapshot Pattern (`SimulationSnapshot`):**
   * Eliminates GUI-engine coupling: The GUI never touches live `Vehicle`, `Road`, or `TrafficSignal` instances.
   * All numbers on screen (ticks, signals, queue counts, vehicles) come from one single atomic snapshot captured under lock.
   * Zero tearing: No chance that the North road is painted from tick $N$ and the South road from tick $N+1$.
4. **Safe Inter-Thread Communication & Coordination:**
   * Uses `volatile boolean running` and `volatile boolean paused` to guarantee immediate cross-thread visibility without CPU cache staleness.
   * **Pause / Resume:** When paused, worker thread calls `pauseLock.wait()` inside a `synchronized (pauseLock)` block, releasing CPU execution and entering the `WAITING` state. Calling `resume()` invokes `pauseLock.notifyAll()`, waking up the worker immediately.
   * **Safe Termination:** `stop()` sets `running = false`, wakes up the thread if waiting, calls `workerThread.interrupt()` to cleanly break out of `Thread.sleep()`, and permits caller to `join()` until the thread dies. Never calls deprecated `Thread.stop()`.

---

## 5. Implemented Simulation Logic & Priority Rules

1. **Normal Round-Robin Cycle:**
   * Signals cycle clockwise: **North** &rarr; **East** &rarr; **South** &rarr; **West**.
   * Only **one road** is `GREEN` at any time; all three other roads remain `RED` (guaranteeing zero intersection conflicts).
   * Strict transition sequence: `GREEN` &rarr; `YELLOW` (clearance period) &rarr; `RED` &rarr; next road `GREEN`.
2. **Emergency Vehicle Priority Rules:**
   * **Approach Preemption:** On every tick, the `SimulationEngine` scans incoming approaches for ambulances (`findHighestUrgencyRoadIndex()`). If an ambulance is detected on another road, the active green road is **safely curtailed to YELLOW** (allowing moving cars to decelerate safely). Once YELLOW expires and turns RED, the road with the **highest urgency level (1–5)** is granted `GREEN`.
   * **Lane Queue Priority:** If an ambulance arrives on an approach behind cars or buses, ordinary traffic yields and the ambulance is served first (`Road.removeVehicle()`).
3. **Starvation Prevention:**
   * Tracks `waitingTime` specifically for **ordinary commuter vehicles** on non-green roads (independent of emergency vehicles).
   * Default threshold is **20 seconds**; the console demonstration explicitly configures it to **15 seconds** for a compact demonstration.
   * If any ordinary commuter vehicle reaches the threshold, normal commuter traffic is served first before granting further emergency overrides.

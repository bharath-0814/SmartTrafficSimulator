# System Architecture Document

## 1. Project Overview & Philosophy

The **Smart Traffic Management and Emergency Vehicle Prioritization Simulator** is designed as an educational, beginner-friendly Java application for university students studying **CSC2104: Object Oriented Programming**.

Unlike complex industrial simulations with complicated coordinate geometry or micro-routing, this system models **one single four-way intersection** with simple, deterministic rules that are clean to code, easy to debug, and straightforward to explain during a university viva voce examination.

> **Current Implementation Status:**  
> • **Phase 1 (Foundation):** COMPLETED  
> • **Phase 2 (Core OOP Models):** COMPLETED — All domain classes (`Vehicle`, `Car`, `Bike`, `Bus`, `EmergencyVehicle`, `Ambulance`, `EmergencyCapable`, `TrafficSignal`, `Road`, `Intersection`) are fully implemented and verified.  
> • **Phase 3 (Simulation Logic):** COMPLETED — `SimulationEngine` console state machine, preemption, and starvation protection verified with 23 tests.  
> • **Phase 4 (File Handling & Exceptions):** COMPLETED — `ReportService` and `ReportException` verified with 30 tests.  
> • **Phase 5 (Multithreading):** COMPLETED — `SimulationController` (`Runnable`, background `Thread`, `wait()`/`notifyAll()` pause/resume, `join()`, `volatile` flags) verified with 41 tests.  
> • **Phase 6 (GUI Visualizer):** COMPLETED — `MainFrame` and `IntersectionPanel` verified with 46 tests.  

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

## 3. Simplified Layered Architecture

```
+-------------------------------------------------------------+
|            ui (Presentation) — [Implemented: Phase 6]       |
|       MainFrame (Dashboard), IntersectionPanel (2D Canvas)  |
+------------------------------+------------------------------+
                               | calls / renders
                               v
+-------------------------------------------------------------+
|            service (Services) — [Implemented: Phase 4]      |
|           ReportService (FileWriter / BufferedReader)       |
|           ReportException (Custom Checked Exception)        |
+---------------+------------------------------+--------------+
                |                              |
                v                              v
+-------------------------------+   +-------------------------+
|  engine (Core) [Implemented]  |   | model [Implemented:Ph 2]|
| SimulationEngine              |   | Vehicle, Car, Bike,     |
| State machine & preemption    |   | Bus, EmergencyVehicle,  |
| SimulationController          |   | Ambulance, Road, Signal,|
| (Runnable, background thread) |   | Intersection, Direction |
+---------------+---------------+   +-------------------------+
                |                               ^
                +----------> updates -----------+
```

### Layer Breakdown & Implementation Status:

1. **`com.traffic.model` (Domain Objects) — [IMPLEMENTED IN PHASE 2]**
   * Plain Java classes holding data, encapsulation, and basic operations.
   * `Vehicle` (abstract base class), `Car`, `Bike`, `Bus`, `EmergencyVehicle`, and `Ambulance`.
   * `EmergencyCapable` interface for sirens and urgency ratings.
   * `Road` (holds an `ArrayList<Vehicle>` queue, defensive copying, lane-level emergency queue priority, and capacity limits).
   * `TrafficSignal` (manages `RED`, `YELLOW`, `GREEN` states, green/yellow durations, and emergency override).
   * `Intersection` (aggregates the 4 roads: North, East, South, West).
   * **Rule:** Zero UI code; zero Swing imports.

2. **`com.traffic.engine` (Simulation Logic & Threading) — [IMPLEMENTED IN PHASES 3 & 5]**
   * `SimulationEngine`: Drives the discrete simulation loop (`tick()` and `step()`).
   * Manages round-robin rotation (North &rarr; East &rarr; South &rarr; West), enforces single-green safety invariant, and handles `GREEN -> YELLOW -> RED` phase transitions.
   * Executes safe emergency preemption without abrupt cuts, prioritizes highest-urgency ambulances, and enforces anti-starvation thresholds for normal commuter traffic.
   * Increments waiting times strictly for vehicles stopped at RED signals.
   * `SimulationController`: Implements `Runnable` to run the simulation on a dedicated background `Thread`. Provides thread-safe `start()`, `pause()`, `resume()`, and `stop()` operations via `wait()` and `notifyAll()` with zero CPU consumption during pause, and controls tick pacing with `Thread.sleep(tickDelayMs)`.
   * Zero UI dependencies; tested through console and JUnit 5 tests.

3. **`com.traffic.service` (Utility & File Services) — [IMPLEMENTED IN PHASE 4]**
   * `ReportService`: Handles file writing (`FileWriter`) and reading (`FileReader`, `BufferedReader`) to format and save simulation summaries to `traffic_report.txt`.
   * `ReportException`: Custom checked exception extending `java.lang.Exception` wrapping low-level I/O errors.
   * Guarantees safe stream closure via `finally` blocks.

4. **`com.traffic.ui` (User Interface) — [IMPLEMENTED IN PHASE 6]**
   * `MainFrame`: The primary Swing dashboard window with dark styling, real-time control buttons (Start, Pause, Resume, Stop, Step Once), simulation speed slider, interactive vehicle injector, live metrics counters, and report generation/viewer dialogs.
   * `IntersectionPanel`: Custom 2D painting (`paintComponent`) rendering the top-down 4-way crossroads, yellow center dividers, stop lines, traffic signal housings with glowing bulbs/timers, and color-coded vehicles (including ambulances with a distinct red emergency cross).
   * Refreshed strictly on the Swing Event Dispatch Thread (EDT) at 20 Hz (50ms interval) via `javax.swing.Timer`.
   * **Rule:** UI sends commands to the controller/engine/service and reads current states for rendering; zero traffic logic inside UI classes.

5. **`com.traffic.app` (Application Entry Point) — [IMPLEMENTED IN PHASES 1, 3, 4, 5 & 6]**
   * `Main.java`: Contains `public static void main(String[] args)`, executes the multi-act console demonstration by default, and supports launching the Swing GUI with the `--gui` flag.

---

## 4. Multithreading Model (Phase 5 — Implemented)

A common pitfall in beginner Java GUI projects is running long simulation loops directly on the Swing Event Dispatch Thread (EDT), which freezes the user interface.

Our design separates threads cleanly in Phase 5 via `SimulationController`:

1. **Main / Caller Thread (or Swing Event Dispatch Thread in Phase 6):**
   * Instantiates `SimulationController` and manages user commands (`start()`, `pause()`, `resume()`, `stop()`).
   * In Phase 6, listens to button clicks and repaints the canvas at visual frame rates.
2. **Simulation Background Worker Thread (`SimulationController implements Runnable`):**
   * Created as a dedicated daemon/worker thread: `new Thread(this, "TrafficSimulation-WorkerThread")`.
   * Executes the `run()` loop: calls `engine.tick()`, sleeps via `Thread.sleep(tickDelayMs)`.
3. **Safe Inter-Thread Communication & Coordination:**
   * Uses `volatile boolean running` and `volatile boolean paused` to guarantee immediate cross-thread visibility without CPU cache staleness.
   * **Pause / Resume:** When paused, worker thread calls `pauseLock.wait()` inside a `synchronized (pauseLock)` block, releasing CPU execution and entering the `WAITING` state. Calling `resume()` invokes `pauseLock.notifyAll()`, waking up the worker immediately.
   * **Safe Termination:** `stop()` sets `running = false`, wakes up the thread if waiting, calls `workerThread.interrupt()` to cleanly break out of `Thread.sleep()`, and permits caller to `join()` until the thread dies. Never calls deprecated `Thread.stop()`.

---

## 5. Implemented Simulation Logic & Priority Rules (Phase 3)

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

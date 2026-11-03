# Smart Traffic Management & Emergency Vehicle Prioritization Simulator

> **University OOP Mini-Project (Java)**  
> **Course:** CSC2104 — Object Oriented Programming (3rd Semester B.Tech CSE)  
> **Institution:** Manipal University Jaipur — Department of Computer Science & Engineering  

---

## 1. Project Overview

In urban environments, emergency vehicles like ambulances frequently face critical delays at busy intersections. 

This project simulates a **single four-way traffic intersection** (North, South, East, and West roads) governed by automatic traffic signals. Under normal conditions, traffic signals cycle in a round-robin schedule. When an **Ambulance** arrives on any incoming approach, the simulator automatically prioritizes it by turning its signal **GREEN** while keeping all conflicting approaches **RED**, allowing immediate safe passage.

The application is written in clean, beginner-friendly Java and demonstrates maximum alignment with the **CSC2104 course syllabus**.

---

## 2. Key Features

* **Single 4-Way Intersection:** Models 4 approaches (North, East, South, West) with dedicated vehicle queues and strict encapsulation *(Phase 2 - Implemented)*.
* **Vehicle Hierarchy:** Models `Car`, `Bike`, `Bus`, and `Ambulance` using meaningful single, hierarchical, and multilevel inheritance with runtime polymorphism *(Phase 2 - Implemented)*.
* **Intelligent Traffic Lights:** Cycles `RED`, `YELLOW`, `GREEN` with controlled durations and emergency state transitions *(Phase 2 - Implemented)*.
* **Emergency Vehicle Queue Priority:** If an ambulance arrives on an approach behind cars or buses, ordinary traffic yields and the ambulance is served first upon receiving GREEN *(Phase 3 - Implemented)*.
* **Simulation Loop (Console):** Round-robin cycling, safe `GREEN -> YELLOW -> RED` transitions, highest-urgency emergency preemption, and anti-starvation protection for ordinary commuter vehicles (default threshold: 20 seconds; explicitly configured to 15 seconds in the console demo) *(Phase 3 - Implemented)*.
* **File Reports & Custom Exception:** Generates and persists detailed simulation summaries to `traffic_report.txt` using `FileWriter`, reads back with `FileReader` and `BufferedReader`, safely closes resources in `finally` blocks, and handles errors via custom checked `ReportException` *(Phase 4 - Implemented)*.
* **Multithreading & Thread Lifecycle:** Background simulation worker thread using `SimulationController implements Runnable`, `Thread.sleep()` interval control, safe `pause()` and `resume()` via `wait()` / `notifyAll()`, and safe termination via `stop()` and `join()` without deprecated methods *(Phase 5 - Implemented)*.
* **Visual Swing GUI Dashboard:** Interactive 2D top-down crossroads canvas rendering asphalt roads, yellow lane dividers, stop lines, glowing traffic lights with countdown timers, vehicle bodies (cars, buses, bikes, ambulances with red cross), real-time control buttons, live metrics, vehicle injection, and report viewer dialog *(Phase 6 - Implemented)*.

---

## 3. Simplified Project Structure

```text
SmartTrafficSimulator/
│
├── pom.xml                                   # Clean Maven configuration (Java 17+, JUnit 5)
├── mvnw / mvnw.cmd                           # Portable Maven Wrapper scripts (LF for Linux, CRLF for Windows)
├── README.md                                 # Project overview & running instructions
├── traffic_report.txt                        # Generated simulation audit report (Phase 4)
├── .gitignore                                # Git ignore patterns
│
├── docs/                                     # Academic & Viva Voce Documentation
│   ├── LEARN_THE_PROJECT.md                  # Plain-English step-by-step code walkthrough
│   ├── syllabus-coverage.md                  # Comprehensive mapping to all 48 lectures
│   ├── architecture.md                       # Simplified 4-way intersection architecture & threads
│   ├── class-diagram.md                      # Simple Mermaid class diagram
│   └── viva-questions.md                     # High-yield viva questions with clear answers
│
└── src/
    ├── main/
    │   ├── java/com/traffic/
    │   │   ├── app/                          # Main.java application launcher (Implemented)
    │   │   ├── model/                        # Domain models (Phase 2 - Implemented):
    │   │   │                                 # - Vehicle (Abstract base)
    │   │   │                                 # - Car, Bike, Bus (Subclasses)
    │   │   │                                 # - EmergencyCapable (Interface)
    │   │   │                                 # - EmergencyVehicle (Abstract intermediate)
    │   │   │                                 # - Ambulance (Multilevel subclass)
    │   │   │                                 # - SignalColor, Direction (Enums)
    │   │   │                                 # - TrafficSignal, Road, Intersection
    │   │   ├── engine/                       # SimulationEngine, SimulationController (Phase 3 & 5 - Implemented)
    │   │   ├── service/                      # ReportService, ReportException (Phase 4 - Implemented)
    │   │   └── ui/                           # MainFrame, IntersectionPanel (Phase 6 - Implemented)
    │   │
    │   └── resources/                        # Icons and configuration assets
    │
    └── test/
        └── java/com/traffic/                 # JUnit 5 unit tests (46 automated tests pass)
```

---

## 4. Academic Alignment with CSC2104 Syllabus

The codebase is specifically organized to demonstrate the concepts taught in the official course handout:

| Syllabus Area | Lectures Covered | Concepts Demonstrated in Project |
| :--- | :---: | :--- |
| **OOP Fundamentals & Control Flow** | Lect 1–7 | Classes, objects, data types, `if-else`, `switch`, state machine, simulation loop *(Implemented)* |
| **Arrays & Methods** | Lect 8–16 | Fixed `Road[4]` array, method overloading, call-by-value *(Implemented)* |
| **Encapsulation & Access Control** | Lect 17–21 | `private` variables, public getters/setters, defensive copies, `final` constants *(Implemented)* |
| **Inheritance & Polymorphism** | Lect 22–31 | `abstract class Vehicle`, multilevel inheritance (`Vehicle` &rarr; `EmergencyVehicle` &rarr; `Ambulance`), `super()`, method overriding, runtime dynamic dispatch, `EmergencyCapable` interface *(Implemented)* |
| **Console & File I/O** | Lect 32–35 | Writing simulation metrics via `FileWriter`, reading line-by-line via `FileReader` & `BufferedReader`, guaranteed resource cleanup in `finally` blocks *(Implemented)* |
| **Exception Handling** | Lect 36–39 | `try`, `catch`, `finally` constructs; custom checked `ReportException` extending `java.lang.Exception`; exception chaining (`super(message, cause)`); `throw` and `throws` *(Implemented)* |
| **Multithreading & Synchronization**| Lect 40–48 | `SimulationController implements Runnable`, background worker `Thread`, `Thread.sleep()`, safe `pause()`/`resume()` via `wait()`/`notifyAll()`, `join()`, `volatile` boolean flags *(Implemented)* |

Detailed lecture-by-lecture breakdown: **[docs/syllabus-coverage.md](docs/syllabus-coverage.md)**.  
Viva questions and answers: **[docs/viva-questions.md](docs/viva-questions.md)**.

---

## 5. How to Build & Run

### Prerequisites
* **Java Development Kit (JDK) 17 or newer**:
  ```bash
  java -version
  ```

### Build & Run Tests
Compile the project and run all automated JUnit 5 tests (46 unit tests):
* **Windows (PowerShell / CMD):**
  ```powershell
  .\mvnw.cmd test
  ```
* **Linux / macOS:**
  ```bash
  ./mvnw test
  ```

### Launch the Java Swing GUI Dashboard (Interactive Visualizer)
Launch the interactive graphical dashboard with the 2D intersection canvas and control sidebar:
* **Option A: Via Maven Wrapper (Direct GUI):**
  ```powershell
  .\mvnw.cmd compile exec:java -Dexec.mainClass="com.traffic.ui.MainFrame"
  ```
* **Option B: Via Main with `--gui` flag:**
  ```powershell
  .\mvnw.cmd compile exec:java -Dexec.mainClass="com.traffic.app.Main" -Dexec.args="--gui"
  ```
* **Option C: Directly via Java Bytecode:**
  ```powershell
  java -cp target/classes com.traffic.ui.MainFrame
  ```

### Run the Console Simulation & Generate Report
Run the headless console demonstration and report generator:
```powershell
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.traffic.app.Main"
```
Or directly via java:
```powershell
java -cp target/classes com.traffic.app.Main
```

---

## 6. Incremental Development Roadmap

* [x] **Phase 1: Clean Foundation & Simplified Architecture** *(Completed)*
* [x] **Phase 2: Basic Java OOP Models** (`Vehicle`, `Car`, `Bike`, `Bus`, `EmergencyCapable`, `EmergencyVehicle`, `Ambulance`, `Road`, `TrafficSignal`, `Intersection`) *(Completed & Verified)*
* [x] **Phase 3: Traffic Simulation Logic** (`SimulationEngine` state machine in console, round-robin cycling, lane-level queue priority, safe preemption, starvation protection) *(Completed & Verified with 23 Tests)*
* [x] **Phase 4: File Handling & Exception Handling** (`ReportService`, custom `ReportException`, `FileWriter`, `FileReader`, `BufferedReader`, `finally` cleanup) *(Completed & Verified with 30 Tests)*
* [x] **Phase 5: Multithreading** (`SimulationController implements Runnable`, background `Thread`, `Thread.sleep()`, thread-safe `pause()`/`resume()`, `stop()`, `join()`, `volatile` flags) *(Completed & Verified with 41 Tests)*
* [x] **Phase 6: Swing GUI & Visualization** (`MainFrame`, `IntersectionPanel`, top-down 2D canvas, live controls, vehicle injection, report viewer) *(Completed & Verified with 46 Tests)*
* [x] **Phase 7: Testing, Polish & Viva Preparation** (Comprehensive unit test verification and final viva review)

# Master Beginner's Guide: Learning the Smart Traffic Simulator from Zero

> **Course:** CSC2104 — Object Oriented Programming (3rd Semester B.Tech CSE)  
> **Target Audience:** Absolute beginner in Java & OOP.  
> **Verified Test Suite:** 52 Automated Unit Tests (100% Passing)  
> **Purpose:** Teach you the entire codebase from absolute scratch so you can confidently code, debug, explain, and defend this project in your university viva voce.

---

## Welcome! How to Study This Project

If you have never written a large Java application before, do not worry. This project was carefully designed to prioritize **understandability, clean code, and syllabus concepts** over complicated architectural patterns.

There are no confusing design patterns, no complex lambda chains, and no external frameworks. Everything is written using **pure, standard Java (JDK 17+)** and standard Java Swing.

This guide is organized into 6 clear sections:
* **Section A: Java Foundations** — The core Java building blocks you must know first.
* **Section B: OOP Concepts in This Project** — How the 4 pillars and OOP principles are applied in our code.
* **Section C: Complete Project Architecture & Class Breakdown** — Detailed explanation of every single Java file.
* **Section D: Advanced Concepts Used** — File handling, custom exceptions, multithreading, and Swing GUI.
* **Section E: High-Yield Viva Preparation** — Questions professors ask, with natural, conversational answers.
* **Section F: Syllabus Gap Report** — Comparison against the CSC2104 syllabus (what is demonstrated and what is taught theoretically).

---

# SECTION A: Java Foundations

Before diving into traffic lights and cars, let's understand how Java works.

### 1. What is Java?
Java is a high-level, class-based, object-oriented programming language. It is famous for its principle: **"Write Once, Run Anywhere" (WORA)**.
* In C/C++, source code compiles directly into machine code for a specific operating system (e.g., Windows x64). An `.exe` file compiled on Windows cannot run on Linux or Mac.
* In Java, your `.java` source code is compiled by the compiler (`javac`) into an intermediate form called **Bytecode** (`.class` files).
* The **Java Virtual Machine (JVM)** on your computer reads the bytecode and executes it as native instructions for your specific operating system.

### 2. How Java Programs Execute
```mermaid
flowchart LR
    Source["Main.java (Source Code)"] -->|"javac (Compiler)"| Bytecode["Main.class (Bytecode)"]
    Bytecode -->|"java (JVM)"| Execution["Operating System & CPU Execution"]
```
Every standalone Java program begins execution at a specific entry point:
```java
public static void main(String[] args)
```
When you run `java com.traffic.app.Main`, the JVM loads the `Main` class into memory and automatically executes this method.

### 3. Classes and Objects
* **Class:** A template or blueprint that defines the structure and behaviors of something.  
  *Example:* `Car.java` is a blueprint. It describes what a car has (speed, passengers) and what a car can do (`displayInfo()`).
* **Object:** An actual, living instance created from that blueprint in computer memory.  
  *Example:* `new Car("CAR-1", 45, 2)` allocates memory on the heap for a specific car with ID "CAR-1".

### 4. Variables and Data Types
In Java, every variable must have a declared data type. Java data types fall into two categories:
1. **Primitive Data Types:** Store raw values directly in memory (fast, stack-allocated):
   * `int`: 32-bit whole numbers (`int speed = 50;`, `int timer = 10;`).
   * `boolean`: true or false (`boolean emergency = false;`).
   * `double`: 64-bit decimal numbers.
2. **Reference Data Types:** Store the memory address of an object on the heap:
   * `String`: Text data (`String id = "CAR-1";`).
   * Arrays: Fixed collections (`Road[] roads = new Road[4];`).
   * Objects: Instances of classes (`Vehicle v`, `TrafficSignal signal`).

### 5. Methods
A method is a reusable block of code inside a class that performs an action:
```java
public void switchToGreen() {
    this.color = SignalColor.GREEN;
    this.timer = greenDuration;
}
```
* `public`: Access modifier (anyone can call this method).
* `void`: Return type (it does not return any value back).
* `switchToGreen()`: Method name and parameter list.

### 6. Constructors
A constructor is a special method called automatically when an object is created using `new`.
* It always has the **exact same name as the class**.
* It has **no return type** (not even `void`).
* Its job is to initialize the object's variables and validate input.
```java
public TrafficSignal(int greenDuration, int yellowDuration) {
    if (greenDuration <= 0) {
        throw new IllegalArgumentException("Green duration must be positive: " + greenDuration);
    }
    this.greenDuration = greenDuration;
    this.yellowDuration = yellowDuration;
    this.color = SignalColor.RED; // Signals start RED by default
}
```

### 7. The `this` and `super` Keywords
* **`this`:** Refers to the current object. Used to distinguish between instance variables and constructor parameters when they have the same name:
  `this.speed = speed;`
* **`super`:** Refers to the parent (super) class. Used in a child constructor to call the parent's constructor:
  `super(id, speed);`  
  *(Rule: In a constructor, `super()` must always be the very first line).*

### 8. The `static` Keyword
* **Instance variable/method:** Belongs to an individual object. Each car has its own distinct `speed`.
* **Static variable/method:** Belongs to the class itself, shared across all instances.
  *Example:* `public static final int DEFAULT_MAX_CAPACITY = 20;` in `Road.java`. There is only one copy in memory, accessed as `Road.DEFAULT_MAX_CAPACITY`.

### 9. Access Modifiers
Java uses 4 access levels to control visibility:
1. `private`: Accessible **only within the same class**. (Used for fields to protect encapsulation).
2. `default` (no keyword): Accessible by any class within the **same package**.
3. `protected`: Accessible within the same package **and by child subclasses**.
4. `public`: Accessible from **anywhere** in the program.

### 10. Arrays vs. Collections (`ArrayList`)
* **Array (`Road[4]`):** Fixed size, created once and cannot grow or shrink. We use a fixed array of size 4 in `Intersection.java` because a 4-way intersection physically has exactly 4 roads (North, East, South, West).
* **`ArrayList<Vehicle>`:** Resizable, dynamic collection from `java.util`. It grows automatically as vehicles arrive and shrinks as they depart. We use it in `Road.java` for vehicle queues.

---

# SECTION B: OOP Concepts in This Project

Here is how the official syllabus OOP concepts are implemented in our actual code:

```mermaid
flowchart TD
    subgraph Pillars["The 4 OOP Pillars"]
        ENC["1. Encapsulation<br/>(Private fields, public getters, synchronized methods)"]
        ABS["2. Abstraction<br/>(abstract class Vehicle, interface EmergencyCapable)"]
        INH["3. Inheritance<br/>(Car extends Vehicle, Ambulance extends EmergencyVehicle)"]
        POLY["4. Polymorphism<br/>(Overriding displayInfo(), ArrayList<Vehicle> queues)"]
    end
```

### 1. Encapsulation
* **Definition:** Hiding internal data and restricting direct outside tampering by making fields `private` and exposing controlled `public` methods.
* **Where in our project:**
  * In `Vehicle.java`: `private String id; private int speed; private int waitingTime;`
  * In `Road.java`: `private ArrayList<Vehicle> queue;` is private. Outside classes cannot call `queue.remove()` directly. They must call `road.addVehicle(v)` or `road.removeVehicle()`, which enforces capacity limits and thread safety.
  * In `Road.getVehicles()`: Returns `new ArrayList<>(queue)` (**defensive copy**), so outside code modifying the returned list cannot corrupt the road's actual queue.

### 2. Abstraction
* **Definition:** Showing only the essential features of an entity while hiding the implementation complexity.
* **Where in our project:**
  * `Vehicle.java` is an `abstract class`. You cannot do `new Vehicle(...)` because a generic "vehicle" does not exist in the real world; only concrete vehicles like cars, buses, and ambulances exist.
  * `Vehicle` declares abstract methods:
    ```java
    public abstract void displayInfo();
    public abstract int calculatePriority();
    ```
    Every concrete subclass *must* provide its own implementation of these methods.

### 3. Inheritance
* **Definition:** Mechanism where a child class inherits variables and methods from a parent class, promoting code reusability.
* **Where in our project:**
  * `Car extends Vehicle` &rarr; Car inherits `getId()`, `getSpeed()`, `getWaitingTime()`, and `incrementWaitingTime()` from `Vehicle`.
  * `Bike extends Vehicle`
  * `Bus extends Vehicle`

### 4. Multilevel Inheritance
* **Definition:** An inheritance chain where a class inherits from a derived class (Grandparent &rarr; Parent &rarr; Child).
* **Where in our project:**
  ```
  Level 1: Vehicle (Abstract Base Class)
              │  extends
              ▼
  Level 2: EmergencyVehicle (Abstract Intermediate Class)
              │  extends
              ▼
  Level 3: Ambulance (Concrete Leaf Subclass)
  ```
  * `Vehicle`: Defines `id`, `speed`, and `waitingTime`.
  * `EmergencyVehicle`: Adds siren state (`boolean sirenActive`, `activateSiren()`).
  * `Ambulance`: Adds medical attributes (`int urgencyLevel`, `String destinationHospital`).

### 5. Interfaces
* **Definition:** A pure contract specifying a set of method signatures that implementing classes must fulfill.
* **Where in our project:**
  * `EmergencyCapable.java`:
    ```java
    public interface EmergencyCapable {
        void activateSiren();
        void deactivateSiren();
        boolean isSirenActive();
        int getUrgencyLevel();
    }
    ```
  * `Ambulance implements EmergencyCapable`: Ensures that any emergency vehicle can be treated uniformly by the traffic light system without forcing non-emergency vehicles (`Car`, `Bike`) to have siren methods.

### 6. Method Overriding (`@Override`)
* **Definition:** When a subclass provides its own specific implementation of a method that is already defined in its superclass.
* **Where in our project:**
  * In `Car.java`: Overrides `calculatePriority()` to return passenger count + 1.
  * In `Ambulance.java`: Overrides `calculatePriority()` to return `100 + (urgencyLevel * 10)`, giving ambulances top priority.
  * In `Ambulance.java`: Overrides `isEmergency()` to return `true` (base `Vehicle` returns `false`).

### 7. Polymorphism & Dynamic Method Dispatch
* **Definition:** The ability to treat child objects as instances of their parent class, resolving the correct method implementation at runtime based on the actual object on the heap.
* **Where in our project:**
  * In `Road.java`: The queue is defined as `ArrayList<Vehicle>`.
  * This single list can hold `Car`, `Bike`, `Bus`, and `Ambulance` objects together.
  * When `v.displayInfo()` is called in a loop, Java uses **dynamic method dispatch** to automatically call `Car.displayInfo()` for a car or `Ambulance.displayInfo()` for an ambulance.

### 8. Composition ("has-a")
* **Definition:** Designing classes by containing instances of other classes rather than inheriting.
* **Where in our project:**
  * `Road` *has-a* `TrafficSignal` (not a subclass of signal).
  * `Road` *has-a* `ArrayList<Vehicle>` queue.
  * `Intersection` *has-a* `Road[4]` array.
  * If the intersection is destroyed, the 4 roads cease to exist (strong composition).

---

# SECTION C: Project Architecture & Class Breakdown

The complete project follows a clean, decoupled data flow:

```
Main (Launcher)
  │
  ├──> Model (Vehicle, Road, TrafficSignal, Intersection)
  │      │
  │      ▼
  └──> SimulationEngine (Rules, Clock, Emergency Preemption)
         │
         ├──> SimulationController (Background Thread, Runnable)
         │
         ├──> SimulationSnapshot (Immutable Thread-Safe Freeze-Frame)
         │      │
         │      ├──> Swing GUI (MainFrame & IntersectionPanel)
         │      │
         │      └──> ReportService (FileWriter, BufferedReader, ReportException)
```

Here is every single class explained:

---

### 1. `com.traffic.app.Main`
* **What it does:** The main starting point of the application.
* **Why we need it:** The JVM needs a class containing `public static void main(String[] args)` to launch.
* **Key responsibilities:**
  * Parses command-line arguments: if `--gui` is passed, it launches the interactive Swing dashboard (`MainFrame`); otherwise, it executes the comprehensive console simulation demonstration.
* **Interacts with:** `SimulationEngine`, `SimulationController`, `MainFrame`, `ReportService`.

---

### 2. `com.traffic.model.Vehicle` (Abstract)
* **What it does:** The base blueprint for all vehicles in the simulation.
* **Why we need it:** Prevents code duplication by grouping shared vehicle attributes (`id`, `speed`, `waitingTime`) in one place.
* **Important variables:** `private String id`, `private int speed`, `private int waitingTime`.
* **Important methods:** `getId()`, `getSpeed()`, `setSpeed()`, `getWaitingTime()`, `incrementWaitingTime()`, `isEmergency()`, abstract `displayInfo()`, abstract `calculatePriority()`.
* **Interacts with:** Subclasses (`Car`, `Bike`, `Bus`, `EmergencyVehicle`).

---

### 3. `com.traffic.model.Car`, `Bike`, `Bus`
* **What they do:** Concrete vehicle types representing standard commuter traffic.
* **Unique attributes:**
  * `Car`: `private int passengers` (passenger count).
  * `Bike`: `private boolean hasHelmet` (safety gear flag).
  * `Bus`: `private int passengerCapacity` (total passenger seats).
* **Important methods:** Each overrides `displayInfo()` and `calculatePriority()`.

---

### 4. `com.traffic.model.EmergencyCapable` (Interface)
* **What it does:** Defines the contract for any emergency vehicle.
* **Methods:** `activateSiren()`, `deactivateSiren()`, `isSirenActive()`, `getUrgencyLevel()`.
* **Why an interface?** Keeps siren and urgency logic decoupled from standard vehicles.

---

### 5. `com.traffic.model.EmergencyVehicle` (Abstract)
* **What it does:** Intermediate class in the multilevel hierarchy between `Vehicle` and `Ambulance`.
* **Adds:** `private boolean sirenActive`, implements siren toggling, and overrides `isEmergency()` to return `true`.

---

### 6. `com.traffic.model.Ambulance`
* **What it does:** Concrete emergency responder vehicle.
* **Implements:** `EmergencyCapable`.
* **Unique attributes:** `private int urgencyLevel` (1 to 5), `private String destinationHospital`.
* **Validation:** Rejects urgency levels outside 1–5 using `IllegalArgumentException`.

---

### 7. `com.traffic.model.SignalColor` & `Direction` (Enums)
* **`SignalColor`:** Type-safe constants: `RED`, `YELLOW`, `GREEN`. Prevents spelling mistakes (e.g. typing `"gren"`).
* **`Direction`:** Type-safe compass approaches: `NORTH`, `EAST`, `SOUTH`, `WEST`.

---

### 8. `com.traffic.model.TrafficSignal`
* **What it does:** Represents one traffic light box controlling one road approach.
* **Important variables:** `private SignalColor color`, `private int timer`, `private int greenDuration`, `private int yellowDuration`, `private boolean emergencyOverride`.
* **Important methods:** `switchToGreen()`, `switchToYellow()`, `switchToRed()`, `tick()` (decrements timer), `isTimerExpired()`.
* **Thread safety:** All methods are `synchronized`.

---

### 9. `com.traffic.model.Road`
* **What it does:** Represents one approach road leading into the intersection.
* **Composition:** Has a `TrafficSignal` and an `ArrayList<Vehicle>` queue.
* **Important methods:**
  * `addVehicle(Vehicle v)`: Enqueues a vehicle if not full (capacity validation).
  * `removeVehicle()`: **Emergency Queue Priority** &rarr; If an ambulance is anywhere in the queue, it departs first! Otherwise, standard FIFO order.
  * `getVehicles()`: Returns a defensive copy (`new ArrayList<>(queue)`).
* **Thread safety:** All mutation and inspection methods are `synchronized`.

---

### 10. `com.traffic.model.Intersection`
* **What it does:** Aggregates the 4 approaching roads (`Road[] roads = new Road[4]`).
* **Indexing:** 0 = NORTH, 1 = EAST, 2 = SOUTH, 3 = WEST.
* **Methods:** `getRoad(int index)`, `getRoad(Direction dir)`.

---

### 11. `com.traffic.engine.SimulationEngine`
* **What it does:** The heart of the simulation state machine.
* **Key responsibilities:**
  1. Discrete simulation tick loop (`tick()`).
  2. Round-robin signal rotation: North &rarr; East &rarr; South &rarr; West.
  3. Single-green safety invariant: Only 1 approach can be GREEN at a time.
  4. Safe transitions: GREEN &rarr; YELLOW (2s) &rarr; RED &rarr; next road GREEN.
  5. Emergency preemption: Detects ambulances and switches active road to YELLOW safely, then grants GREEN to the highest-urgency ambulance approach.
  6. Anti-starvation rule: If ordinary vehicles wait $\ge$ starvation threshold (20s default; 15s in demo), they are served before new emergency overrides.
  7. `getSnapshot()`: Produces an atomic `SimulationSnapshot` for GUI and reporting.
  8. `addVehicle(approachIdx, vehicle)`: Safe, synchronized vehicle injection.

---

### 12. `com.traffic.engine.SimulationSnapshot`
* **What it does:** An **immutable data transfer object (DTO)** capturing the entire state of the intersection at one specific instant.
* **Contains:**
  * Overall metrics: ticks, vehicles served, emergency served, active road index.
  * `List<RoadSnapshot>`: Immutable snapshot of each road (name, direction, signal color, timer, queue length).
  * `List<VehicleSnapshot>`: Immutable snapshot of each vehicle (ID, type, speed, waiting time, emergency flag, ambulance flag).
* **Why we need it:** Completely eliminates race conditions between the background simulation worker thread and the Swing GUI.

---

### 13. `com.traffic.engine.SimulationController`
* **What it does:** Governs multithreaded simulation execution.
* **Implements:** `java.lang.Runnable`.
* **Important variables:** `private Thread workerThread`, `private volatile boolean running`, `private volatile boolean paused`, `private final Object pauseLock`.
* **Important methods:**
  * `start()`: Spawns `workerThread` and calls `start()`.
  * `pause()`: Sets `paused = true`. Worker thread calls `pauseLock.wait()` (0% CPU usage).
  * `resume()`: Sets `paused = false` and calls `pauseLock.notifyAll()`.
  * `stop()`: Sets `running = false`, interrupts thread, and waits with `join()`.
  * `stepOnce()`: Advances the simulation by exactly 1 tick when paused.
  * `run()`: The loop calling `engine.tick()` and `Thread.sleep(tickDelayMs)`.

---

### 14. `com.traffic.service.ReportException` & `ReportService`
* **`ReportException`:** Custom checked exception (`extends Exception`) representing report generation/reading failures, wrapping underlying `IOException`s.
* **`ReportService`:**
  * `generateReport(SimulationSnapshot snapshot, String filePath)`: Writes formatted audit text using `FileWriter`.
  * `readReport(String filePath)`: Reads saved report line-by-line using `FileReader` and `BufferedReader`.
  * Guarantees stream closure inside `finally` blocks.

---

### 15. `com.traffic.ui.MainFrame` & `IntersectionPanel`
* **`MainFrame extends JFrame`:** Modern dark-themed dashboard window containing control buttons (Start, Pause, Resume, Stop, Step Once), simulation speed slider, vehicle injection controls, live metric badges, and audit report viewing dialog.
* **`IntersectionPanel extends JPanel`:** Custom 2D canvas overriding `paintComponent(Graphics g)` to render the crossroads, asphalt, yellow center dividers, stop lines, glowing traffic lights with countdown timers, and color-coded vehicles.
* **Thread safety:** Updates strictly on the Swing Event Dispatch Thread (EDT) via `javax.swing.Timer` at 20 Hz (50ms), painting purely from immutable `SimulationSnapshot` objects.

---

# SECTION D: Advanced Concepts Used

### 1. File Handling (`FileWriter`, `FileReader`, `BufferedReader`)
* **Character Streams:** Java provides `Reader` and `Writer` classes for 16-bit Unicode character data (text files).
* In `ReportService.java`:
  ```java
  FileWriter writer = null;
  try {
      writer = new FileWriter(filePath);
      writer.write(reportText);
  } catch (IOException e) {
      throw new ReportException("Failed to write report", e);
  } finally {
      if (writer != null) {
          try { writer.close(); } catch (IOException ignored) {}
      }
  }
  ```
* **Why wrap `FileReader` in `BufferedReader`?**  
  `FileReader` reads one character at a time from disk (slow). `BufferedReader` reads a large chunk into an in-memory buffer and provides `readLine()` to read entire lines until `null` is returned (fast).

### 2. Custom Checked Exception (`ReportException`)
* Java has two types of exceptions:
  1. **Unchecked (`RuntimeException`):** e.g., `IllegalArgumentException`, `NullPointerException`. Do not require mandatory try-catch.
  2. **Checked (`Exception`):** e.g., `IOException`. The Java compiler **forces** you to either handle them with `try-catch` or declare them in the method signature with `throws`.
* In `ReportException.java`:
  ```java
  public class ReportException extends Exception {
      public ReportException(String message) {
          super(message);
      }
      public ReportException(String message, Throwable cause) {
          super(message, cause); // Exception chaining!
      }
  }
  ```
  *Exception Chaining:* We wrap the low-level `IOException` inside our domain-specific `ReportException` so the caller understands what business operation failed.

### 3. Multithreading & The Thread Lifecycle
A thread is a lightweight unit of execution within a program.

```mermaid
stateDiagram-v2
    [*] --> NEW : new Thread(runnable)
    NEW --> RUNNABLE : thread.start()
    RUNNABLE --> TIMED_WAITING : Thread.sleep(ms)
    TIMED_WAITING --> RUNNABLE : sleep expires / interrupted
    RUNNABLE --> WAITING : pauseLock.wait()
    WAITING --> RUNNABLE : pauseLock.notifyAll()
    RUNNABLE --> TERMINATED : run() completes / stop()
    TERMINATED --> [*]
```

* **Why use multithreading?** If the simulation loop ran on the main thread or Swing thread, the window would freeze, buttons would stop responding, and the operating system would report "Not Responding".
* **`Runnable` Interface:** Has one method: `void run()`. `SimulationController` implements `Runnable` and passes itself to `new Thread(this)`.

### 4. Thread Synchronization & Inter-Thread Communication
* **`synchronized`:** Ensures **mutual exclusion**. Only one thread can execute a synchronized method or block on the same object monitor at any time.
* **`volatile`:** Guarantees that writes to a variable (`running`, `paused`) are immediately visible to all other threads, bypassing CPU core cache latency.
* **`wait()` and `notifyAll()`:**
  * When paused, the worker thread calls `pauseLock.wait()`. It enters the `WAITING` state and **releases its lock**, consuming 0% CPU.
  * When resumed, another thread calls `pauseLock.notifyAll()`, waking up the worker thread immediately.
* **`sleep(ms)`:** Puts the thread into `TIMED_WAITING` for a fixed duration to control simulation pacing.
* **`join()`:** Calling `thread.join()` makes the calling thread wait until the target thread terminates.

### 5. Swing Event Dispatch Thread (EDT) & Timers
* **The Golden Rule of Swing:** Swing components (`JLabel`, `JButton`, `JPanel`) are **not thread-safe**. All GUI updates must happen on the **Event Dispatch Thread (EDT)**.
* In `MainFrame.java`, we never update labels from the worker thread. Instead, we use `javax.swing.Timer`:
  ```java
  uiRefreshTimer = new Timer(50, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
          updateDashboard(); // Fires on EDT every 50ms (20 Hz)
      }
  });
  uiRefreshTimer.start();
  ```

### 6. The Immutable Snapshot Pattern
To ensure the GUI never encounters tearing or concurrent modification:
1. Every 50ms, the EDT calls `engine.getSnapshot()`.
2. Inside `SimulationEngine`, a single `synchronized` block reads all roads, lights, and queues, copying their state into a `SimulationSnapshot`.
3. The GUI paints strictly from this snapshot. Even if the background thread changes lights or dequeues vehicles a microsecond later, the GUI's current frame remains 100% internally consistent.

---

# SECTION E: High-Yield Viva Preparation

Here are the most common questions professors ask during viva examinations, along with short, accurate, natural answers:

### 1. General Java & OOP Questions
* **Q: What is the difference between an abstract class and an interface?**  
  *Answer:* "An abstract class can have instance variables and concrete methods with state. An interface can only define method contracts (and default/static methods) without instance fields. A class can implement multiple interfaces, but extend only one class."
* **Q: Why are your class variables `private`?**  
  *Answer:* "To enforce encapsulation. Making fields private prevents external code from corrupting internal values directly, forcing access through validated methods."
* **Q: What happens if an exception is thrown in a `try` block before closing a file?**  
  *Answer:* "Execution jumps directly to the `catch` block, skipping any lines below the error in `try`. That is why stream closure is placed in the `finally` block, which is guaranteed to run."
* **Q: What is dynamic method dispatch?**  
  *Answer:* "It is Java's mechanism for runtime polymorphism. When calling an overridden method on a superclass reference, the JVM determines which child version to execute at runtime based on the actual object on the heap."

### 2. Project Architecture & Algorithm Questions
* **Q: Why did you choose a 4-way intersection instead of multiple intersections?**  
  *Answer:* "A single 4-way intersection models all essential traffic control phenomena — round-robin scheduling, clearance intervals, emergency preemption, and queue anti-starvation — while keeping code clean, readable, and free of unnecessary geometric complexity."
* **Q: What happens when an ambulance arrives at a red light while another road is green?**  
  *Answer:* "The engine detects the ambulance. Instead of cutting the green road abruptly, it curtails the green phase immediately to YELLOW (2 seconds). When yellow expires and turns RED, the ambulance's road is switched to GREEN."
* **Q: What is the difference between preemption and queue priority?**  
  *Answer:* "Preemption is road-level (switching the signal to GREEN for that approach). Queue priority is lane-level (inside `Road.removeVehicle()`, an ambulance waiting behind cars is allowed to depart first)."
* **Q: What is starvation, and how does your simulator prevent it?**  
  *Answer:* "Starvation happens if recurring ambulances keep ordinary commuter cars waiting indefinitely. We prevent it by tracking waiting times for ordinary vehicles; if any vehicle reaches the starvation threshold (20 seconds), normal traffic is granted green before further emergency preemption."

### 3. Concurrency & GUI Questions
* **Q: Why didn't you use `Thread.stop()` to stop the simulation?**  
  *Answer:* "`Thread.stop()` is deprecated because it terminates a thread abruptly, potentially leaving shared data structures half-modified. We use cooperative termination with a `volatile boolean running` flag and `interrupt()`."
* **Q: What does the `volatile` keyword do in your controller?**  
  *Answer:* "It guarantees memory visibility. When one thread modifies `running` or `paused`, the updated value is flushed immediately to main memory so other threads see it without CPU cache lag."
* **Q: Why do you need `SimulationSnapshot`?**  
  *Answer:* "To prevent race conditions and screen tearing. If the GUI read live model objects while the background worker thread was advancing ticks, labels and graphics could display conflicting information from different ticks. The snapshot provides an atomic freeze-frame."

---

# SECTION F: Syllabus Gap Report

To demonstrate academic honesty in your university evaluation, here is the exact alignment between our implementation and the CSC2104 syllabus:

### 1. Fully Demonstrated Syllabus Topics (In Code)
* **Classes & Objects:** Complete domain hierarchy (`Vehicle`, `Road`, `TrafficSignal`).
* **Inheritance & Hierarchy:** Single (`Car extends Vehicle`), Hierarchical (`Bike`, `Bus`), and Multilevel (`Vehicle` &rarr; `EmergencyVehicle` &rarr; `Ambulance`).
* **Polymorphism & Overriding:** `displayInfo()`, `calculatePriority()`, dynamic dispatch in `ArrayList<Vehicle>`.
* **Encapsulation & Validation:** `private` fields, defensive copying in `getVehicles()`, `IllegalArgumentException`.
* **Composition:** `Intersection` has 4 `Road`s; `Road` has a `TrafficSignal`.
* **File I/O:** `FileWriter`, `FileReader`, `BufferedReader`, `finally` resource cleanup.
* **Exception Handling:** Custom checked `ReportException`, try-catch-finally, exception chaining.
* **Multithreading:** `Runnable`, background `Thread`, `Thread.sleep()`, `wait()`, `notifyAll()`, `join()`, `volatile`.
* **Java Swing GUI:** `JFrame`, `JPanel`, custom 2D canvas drawing (`paintComponent`), `javax.swing.Timer`, Event Dispatch Thread (EDT).
* **Nested & Inner Classes:** Static nested classes (`SimulationSnapshot.RoadSnapshot`, `VehicleSnapshot`) and anonymous inner classes (`ActionListener`, `ChangeListener`).

### 2. Partially Demonstrated Topics
* **Recursion:** Demonstrated via constructor chaining (`this(...)`, `super(...)`). Iterative loops (`while`, `for`) were deliberately chosen for simulation ticks and FIFO queues because continuous recursive loops would cause `StackOverflowError`.
* **Generics:** Demonstrated via `ArrayList<Vehicle>`, `List<RoadSnapshot>`, `JComboBox<String>`. Custom generic classes (e.g. `class Queue<T>`) were omitted to keep code beginner-friendly.

### 3. Topics Not Demonstrated in Project (and Why)
* **Applets:** Officially deprecated in Java 9 and removed from modern JDKs. Desktop Java Swing (`JFrame`/`JPanel`) is the modern standard.
* **Networking/Sockets:** Omitted because the simulator is a self-contained local desktop application with zero external network dependencies.
* **JDBC / Databases:** Text file persistence (`FileWriter`) was chosen over database drivers so the project runs cleanly on any computer without requiring MySQL/PostgreSQL installation.

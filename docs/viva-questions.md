# CSC2104 Object Oriented Programming — Comprehensive Viva Voce Guide

**Course:** CSC2104 Object Oriented Programming  
**Institution:** Manipal University Jaipur — Department of Computer Science & Engineering  
**Project:** Smart Traffic Management and Emergency Vehicle Prioritization Simulator  

This guide provides targeted questions, clear explanations, and natural, beginner-friendly model answers covering every aspect of the project for your university viva voce examination.

---

## 1. Java Fundamentals

### Q1: What is Java, and why is it called "Platform Independent"?
> **Answer:**  
> "Java is a class-based, object-oriented programming language. It is platform independent because Java source code (`.java`) is compiled by `javac` into bytecode (`.class`), which runs on any operating system that has a Java Virtual Machine (JVM). The philosophy is *'Write Once, Run Anywhere'*."

### Q2: What is the difference between JDK, JRE, and JVM?
> **Answer:**  
> "• **JVM (Java Virtual Machine):** The runtime engine that executes compiled Java bytecode.  
> • **JRE (Java Runtime Environment):** Includes the JVM plus the standard Java libraries needed to run programs.  
> • **JDK (Java Development Kit):** The complete software package for developers, containing the JRE, compiler (`javac`), archiver (`jar`), and debugger."

### Q3: What is the difference between Primitive types and Reference types?
> **Answer:**  
> "Primitive types (like `int`, `boolean`, `double`) store raw values directly on the stack. Reference types (like `Vehicle`, `String`, `ArrayList`) store the memory address of an object allocated on the heap."

### Q4: What does `public static void main(String[] args)` mean?
> **Answer:**  
> "• `public`: Accessible from anywhere by the JVM.  
> • `static`: Can be called without creating an instance of the class first.  
> • `void`: Returns no value.  
> • `main`: The standardized entry point method name recognized by the JVM.  
> • `String[] args`: Array of command-line text arguments passed into the program."

### Q5: What is the difference between `==` and `.equals()`?
> **Answer:**  
> "In Java, `==` compares primitive values or checks if two object references point to the exact same memory address. `.equals()` is a method meant to compare the actual logical content or attributes of two objects."

---

## 2. Core OOP Concepts in This Project

### Q6: What are the four fundamental pillars of OOP, and where are they in your code?
> **Answer:**  
> "1. **Encapsulation:** Protecting internal object state. In `Vehicle`, `Road`, and `TrafficSignal`, fields are `private` and accessed only via getters/setters or synchronized operations.  
> 2. **Inheritance:** Reusing code in an *is-a* relationship. `Car extends Vehicle`, `Ambulance extends EmergencyVehicle`.  
> 3. **Polymorphism:** Handling different objects uniformly. In `Road.java`, `ArrayList<Vehicle>` holds cars, bikes, and ambulances, dynamically invoking `v.displayInfo()` and `v.calculatePriority()` at runtime.  
> 4. **Abstraction:** Hiding complex implementation details. `abstract class Vehicle` defines a common blueprint without concrete details; `interface EmergencyCapable` specifies emergency siren operations."

### Q7: Why did you use an `abstract class` for `Vehicle` instead of an `interface`?
> **Answer:**  
> "Because all vehicles share common mutable state (`id`, `speed`, `waitingTime`) and concrete methods (`getId()`, `setSpeed()`, `incrementWaitingTime()`). In Java, an interface cannot have instance variables (only `public static final` constants). Since every vehicle needs its own mutable speed and waiting counter, an abstract class is required."

### Q8: Why did you create the interface `EmergencyCapable`?
> **Answer:**  
> "To follow the **Interface Segregation Principle**. Regular vehicles like cars and bikes do not have sirens or medical urgency ratings. If we put siren methods inside `Vehicle`, every car would be forced to inherit them. By creating `EmergencyCapable`, only emergency vehicles implement siren operations."

### Q9: Where does your project demonstrate Multilevel Inheritance?
> **Answer:**  
> "In the 3-level hierarchy:  
> `Vehicle` (Level 1: Base vehicle) &rarr; `EmergencyVehicle` (Level 2: Abstract emergency responder) &rarr; `Ambulance` (Level 3: Concrete medical vehicle).  
> In addition, `Ambulance` implements `EmergencyCapable`."

### Q10: What is the difference between Method Overloading and Method Overriding?
> **Answer:**  
> "• **Overloading (Compile-Time):** Same method name with different parameter types in the same class. Example: `Road(name, direction)` and `Road(name, direction, maxCapacity)`.  
> • **Overriding (Runtime):** Subclass provides its own specific implementation of a parent method with the exact same signature. Example: `Ambulance` overrides `displayInfo()` and `calculatePriority()` from `Vehicle`."

### Q11: What is the difference between Inheritance ("is-a") and Composition ("has-a")?
> **Answer:**  
> "• `Car extends Vehicle` is an **is-a** relationship (Inheritance).  
> • `Road` contains a `TrafficSignal` and an `ArrayList<Vehicle>` queue, which is a **has-a** relationship (Composition). A road is not a type of signal; a road *owns* a signal."

---

## 3. Project Architecture & Classes

### Q12: Walk me through the flow of the application.
> **Answer:**  
> "1. `Main.java` launches the program (either the console simulation or GUI dashboard via `--gui`).  
> 2. `Intersection` creates the 4 `Road` objects (North, East, South, West), each with a `TrafficSignal`.  
> 3. `SimulationEngine` manages signal countdowns, round-robin rotation, ambulance preemption, and anti-starvation rules.  
> 4. `SimulationController` implements `Runnable` to run `engine.tick()` on a background worker thread.  
> 5. `SimulationEngine.getSnapshot()` generates an immutable `SimulationSnapshot` containing safe freeze-frames of roads and vehicles.  
> 6. `MainFrame` and `IntersectionPanel` render the visual dashboard from this snapshot on the Swing Event Dispatch Thread (EDT).  
> 7. `ReportService` formats audit metrics from the snapshot and saves them to `traffic_report.txt`."

### Q13: What does the `SimulationSnapshot` class do, and why is it important?
> **Answer:**  
> "In a multithreaded GUI app, if the Swing GUI reads live model objects directly while the background thread is updating them, race conditions and screen tearing occur (e.g. North road is rendered from tick 5, but South from tick 6).  
> To solve this, `SimulationEngine.getSnapshot()` captures a complete, immutable freeze-frame of all metrics, roads, signals, and queues in a single `synchronized` call. The GUI renders exclusively from this safe snapshot without touching live model objects."

### Q14: How does vehicle injection work safely?
> **Answer:**  
> "In `MainFrame`, when a user clicks 'Add Vehicle', the GUI calls `engine.addVehicle(approachIndex, vehicle)`. This method is `synchronized` on the engine, meaning it cannot collide with an active `engine.tick()` on the background thread. It safely adds the vehicle to the road's queue or rejects it if full."

---

## 4. Traffic Simulation Logic & Algorithms

### Q15: What is the round-robin traffic cycle in your project?
> **Answer:**  
> "Signals cycle clockwise: **NORTH &rarr; EAST &rarr; SOUTH &rarr; WEST &rarr; repeat**.  
> The simulator enforces a single-green invariant: exactly one road is GREEN at any time while the other three are RED. When green time expires, the signal transitions: **GREEN &rarr; YELLOW (2 seconds) &rarr; RED &rarr; next road GREEN**."

### Q16: How does the emergency preemption algorithm work?
> **Answer:**  
> "On every tick, `SimulationEngine` checks if an emergency vehicle (ambulance) is waiting on any incoming road.  
> If an ambulance is detected on another road and no road is starving:  
> 1. The active green road's timer is curtailed to **YELLOW** immediately, allowing moving vehicles to clear the junction safely (no abrupt red cuts).  
> 2. Once YELLOW finishes and turns RED, the road with the **highest urgency ambulance (levels 1–5)** is granted **GREEN** immediately."

### Q17: What is the Emergency Vehicle Queue Priority rule?
> **Answer:**  
> "Inside `Road.removeVehicle()`, instead of strictly removing the vehicle at position 0 (FIFO), the method scans the queue. If an ambulance is in line behind normal commuter cars or buses, the ambulance departs first. If multiple ambulances are queued, the one with the highest medical urgency departs first."

### Q18: What is Starvation Prevention, and why is it necessary?
> **Answer:**  
> "If ambulances arrive repeatedly, emergency preemption could keep normal cars and buses waiting forever.  
> To prevent this, `SimulationEngine` tracks waiting time for ordinary commuter vehicles. If any vehicle waits beyond the starvation threshold (default: 20s; configured to 15s in the console demo), ordinary traffic is given priority to clear before further emergency overrides are granted."

---

## 5. File Handling & Custom Exceptions

### Q19: How did you implement file writing and reading in `ReportService`?
> **Answer:**  
> "• **Writing:** Uses `FileWriter`, a 16-bit character stream, to write formatted text summaries to `traffic_report.txt`.  
> • **Reading:** Uses `FileReader` wrapped in a `BufferedReader` to read the report file line-by-line efficiently using `readLine()` until it returns `null`."

### Q20: Why do you need `finally` blocks for stream cleanup?
> **Answer:**  
> "If an exception occurs while reading or writing a file, normal execution jumps straight to the `catch` block. The `finally` block is guaranteed to execute regardless of whether an exception occurs, ensuring that file descriptors are never leaked."

### Q21: What is `ReportException`, and why is it a checked exception?
> **Answer:**  
> "`ReportException` extends `java.lang.Exception`, making it a checked exception. We use it to encapsulate low-level `IOException`s into a clear, domain-specific exception. Because it is checked, the Java compiler forces calling methods to explicitly handle it with `try-catch` or declare it with `throws`."

---

## 6. Multithreading & Concurrency

### Q22: What is the difference between `Thread` and `Runnable`?
> **Answer:**  
> "`Thread` is a concrete Java class that represents an operating system thread of execution. `Runnable` is a functional interface with a single method: `void run()`.  
> Implementing `Runnable` is preferred because Java only allows single class inheritance. If our `SimulationController` extended `Thread`, it could never extend any other class."

### Q23: What is the difference between `start()` and `run()`?
> **Answer:**  
> "Calling `run()` simply executes the method synchronously on the current thread, just like any normal method call. Calling `start()` requests the operating system to allocate a new thread call stack and then invokes `run()` asynchronously on that new thread."

### Q24: What does the `volatile` keyword do?
> **Answer:**  
> "In multithreading, each CPU core can cache variables in its local L1/L2 cache. Marking `running` and `paused` as `volatile` tells the Java Virtual Machine to always read and write directly to main memory, guaranteeing immediate visibility across threads without stale caching."

### Q25: How do `wait()` and `notifyAll()` implement Pause and Resume?
> **Answer:**  
> "When `pause()` is called, the background thread enters a `synchronized (pauseLock)` block and calls `pauseLock.wait()`. This puts the thread to sleep in the OS scheduler with **0% CPU usage** while releasing the lock.  
> When `resume()` is called, the main thread calls `pauseLock.notifyAll()`, which immediately wakes up the sleeping worker thread to resume ticking."

### Q26: Why is `Thread.stop()` deprecated, and how does your simulator terminate cleanly?
> **Answer:**  
> "`Thread.stop()` is deprecated and dangerous because it abruptly kills a thread while it might be halfway through modifying a road queue or traffic light, leaving shared memory permanently corrupted.  
> Our project uses **cooperative cancellation**: `stop()` sets `running = false` and calls `workerThread.interrupt()` to cleanly wake up sleeping threads, allowing the `run()` loop to exit naturally."

---

## 7. Java Swing GUI

### Q27: What is the Event Dispatch Thread (EDT), and why is it important?
> **Answer:**  
> "The Event Dispatch Thread is the dedicated background thread in Java Swing responsible for handling user input events (clicks, key presses) and painting components. Swing components are **not thread-safe**. Modifying UI labels or repainting panels from background worker threads can cause race conditions, UI freezing, and deadlocks."

### Q28: How does `IntersectionPanel` draw the 4-way intersection?
> **Answer:**  
> "It overrides `paintComponent(Graphics g)` and casts `g` to `Graphics2D` with anti-aliasing enabled. It draws:  
> 1. Asphalt roads and the center junction box using rectangles.  
> 2. Yellow dashed dividing lines and solid white stop lines.  
> 3. Traffic light boxes with glowing RED, YELLOW, or GREEN bulb circles and countdown timers.  
> 4. Color-coded vehicle boxes (blue cars, orange buses, purple bikes, and white ambulances with a red cross)."

### Q29: Why do you call `super.paintComponent(g)` first?
> **Answer:**  
> "Calling `super.paintComponent(g)` cleans the panel's background before drawing the new frame. If omitted, previous drawings would smear and leave visual artifacts as vehicles move."

---

## 8. Tricky Viva Questions a Professor Might Ask

### Q30: "Can you create an instance of an abstract class using `new`?"
> **Answer:**  
> "No, abstract classes cannot be instantiated directly with `new`. They can only be instantiated through their concrete subclasses (e.g. `new Car(...)`, `new Ambulance(...)`)."

### Q31: "Why don't you have a `destructor` in Java like C++?"
> **Answer:**  
> "Java does not have destructors because memory management is automated. The JVM has an automatic **Garbage Collector (GC)** that tracks unreferenced objects on the heap and reclaims their memory automatically."

### Q32: "What is defensive copying, and where did you use it?"
> **Answer:**  
> "Defensive copying is returning a new copy of an internal collection rather than a direct reference to the original collection.  
> In `Road.java`, `getVehicles()` returns `new ArrayList<>(queue)`. If outside code calls `.clear()` on the returned list, the road's actual queue remains completely untouched."

### Q33: "What happens if an exception is thrown in a `try` block before `writer.close()`?"
> **Answer:**  
> "If `writer.close()` were in the `try` block, it would be skipped when an exception occurs, leaking the file handle. Because we placed `writer.close()` inside the `finally` block, it is guaranteed to execute regardless of whether an exception was thrown."

### Q34: "Can a constructor be `final`, `static`, or `abstract`?"
> **Answer:**  
> "No:  
> • Not `final`: Constructors are not inherited, so they cannot be overridden.  
> • Not `static`: Constructors belong to a specific instance being initialized.  
> • Not `abstract`: Constructors must have a concrete body to initialize object fields."

### Q35: "Why did you use `Collections.unmodifiableList()` in `SimulationSnapshot`?"
> **Answer:**  
> "To guarantee immutability. If any caller tries to call `.add()` or `.remove()` on the snapshot's roads or vehicles list, Java immediately throws an `UnsupportedOperationException`. This guarantees that snapshot data cannot be modified by the GUI or any other thread."

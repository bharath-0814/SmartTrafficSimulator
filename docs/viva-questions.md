# CSC2104 Object Oriented Programming — Comprehensive Viva Guide

**Course:** CSC2104 Object Oriented Programming  
**Institution:** Manipal University Jaipur — Department of Computer Science & Engineering  
**Project:** Smart Traffic Management and Emergency Vehicle Prioritization Simulator  

This guide provides targeted questions and simple, clear, student-friendly model answers covering the theoretical concepts and practical implementation of your university mini-project.

> **Study Guide Note:**
> * **Questions 1–11:** Cover Object-Oriented Programming pillars, language fundamentals, validation, and encapsulation implemented in **Phase 2 (Domain Models)**.
> * **Questions 12–23:** Cover advanced curriculum topics (Exception Handling, File I/O, Multithreading, Synchronization, and Simulation Logic) scheduled for upcoming development phases (Phases 3–6).

---

## 1. Core Object-Oriented Programming (OOP) Pillars

### Q1: What are the four fundamental pillars of Object-Oriented Programming, and how does your project demonstrate each?
> **Answer:**
> 1. **Encapsulation:** Binding data and methods into a single class while restricting direct external access.  
>    *In Project:* Instance variables in `Vehicle`, `Road`, and `TrafficSignal` are `private`. Access is controlled through `public` getters, setters, and operational methods (`addVehicle()`, `changeColor()`).
> 2. **Inheritance:** Code reusability where child classes derive properties and methods from a parent class.  
>    *In Project:* Subclasses `Car`, `Bike`, `Bus`, and `EmergencyVehicle` inherit common attributes (`id`, `speed`, `waitingTime`) from abstract class `Vehicle`.
> 3. **Polymorphism:** The ability of different objects to respond to the same method invocation in distinct ways.  
>    *In Project:* Runtime dynamic dispatch when `vehicle.displayInfo()` or `vehicle.calculatePriority()` is called on a collection of `Vehicle` references.
> 4. **Abstraction:** Exposing only essential interface details while hiding internal complexities.  
>    *In Project:* `abstract class Vehicle` defines a template without specifying concrete vehicle details; `interface EmergencyCapable` specifies emergency behaviors (`activateSiren()`).

---

### Q2: Why did you make `Vehicle` an `abstract class` instead of an `interface`?
> **Answer:**
> An **abstract class** is used because all vehicles share common mutable state (`id`, `speed`, `arrivalTime`, `waitingTime`) and non-trivial shared methods (`getId()`, `incrementWaitingTime()`, `getSpeed()`).  
> In Java, an interface cannot maintain instance fields (its fields are strictly `public static final` constants). Since every vehicle needs to store its own independent speed, arrival time, and waiting time, an abstract base class is the correct design.

---

### Q3: Why did you create an interface `EmergencyCapable` instead of putting siren methods in `Vehicle`?
> **Answer:**
> To adhere to the **Interface Segregation Principle**:
> * Standard vehicles (`Car`, `Bike`, `Bus`) do not have sirens or urgency levels. Putting siren methods in the base `Vehicle` class would force regular cars to inherit methods they cannot use.
> * An interface represents a capability. By defining `EmergencyCapable`, only emergency units (`Ambulance`, `FireTruck`) implement siren operations.

---

### Q4: Explain the difference between Method Overloading and Method Overriding with examples from your project.
> **Answer:**
> * **Method Overloading (Compile-Time Polymorphism):** Defining methods in the same class with the same name but different parameter lists.  
>   *Example in Project:* In `Road.java`:
>   ```java
>   public void addVehicle(Vehicle v) { ... }
>   public void addVehicle(String id, int speed) { ... }
>   ```
> * **Method Overriding (Runtime Polymorphism):** A subclass provides its own specific implementation for a method already defined in its superclass.  
>   *Example in Project:* `Vehicle` declares `abstract void displayInfo();`, and `Car` and `Ambulance` override it with their own unique printouts.

---

### Q5: What is Dynamic Method Dispatch (Runtime Polymorphism) and how does it work?
> **Answer:**
> It is the mechanism by which a call to an overridden method is resolved at runtime rather than compile-time.  
> When a superclass reference variable holds a subclass object (e.g., `Vehicle v = new Ambulance("AMB-1", 70);`), calling `v.calculatePriority()` executes the `Ambulance` class's overridden version. The JVM uses the object's method table (`vtable`) on the heap to look up the correct implementation at runtime.

---

### Q6: What is Multilevel Inheritance and where is it in your project?
> **Answer:**
> Multilevel inheritance occurs when a derived class inherits from another derived class, forming a chain:
> $$\text{Vehicle (Base)} \longrightarrow \text{EmergencyVehicle (Derived)} \longrightarrow \text{Ambulance (Subclass)}$$
> * `Vehicle` provides core properties (`id`, `speed`).
> * `EmergencyVehicle` adds emergency behavior (`sirenActive`, `urgencyLevel`).
> * `Ambulance` adds medical properties (`hospitalName`).

---

### Q7: Explain "Inheritance vs Composition" ("is-a" vs "has-a") in your project.
> **Answer:**
> * **Inheritance ("is-a"):** Used when a class is genuinely a specialized version of another.  
>   *Example:* A `Car` **is-a** `Vehicle`.
> * **Composition ("has-a"):** Used when a class contains or coordinates other classes.  
>   *Example:* A `Road` **has-a** `TrafficSignal` and a list of `Vehicle`s. An `Intersection` **has-a** set of 4 `Road`s.  
> *Composition is preferred here because a road is not a type of signal; it contains and controls a signal.*

---

## 2. Java Language Fundamentals

### Q8: What is the role of `super()` and `this()` in Java constructors?
> **Answer:**
> * `super()` invokes the constructor of the direct parent class to initialize inherited fields. It must be the very first statement in a subclass constructor.
> * `this()` invokes an overloaded constructor within the same class (constructor chaining) to avoid code duplication.

---

### Q9: What are the three meanings of the `final` keyword in Java?
> **Answer:**
> 1. **`final` Variable:** Its value cannot be reassigned once initialized (e.g., `final String id;` makes vehicle ID immutable).
> 2. **`final` Method:** Cannot be overridden by any subclass (protects critical logic).
> 3. **`final` Class:** Cannot be inherited/extended by any subclass (e.g., `java.lang.String`).

---

### Q10: What is the difference between `==` and `.equals()`?
> **Answer:**
> * `==` is a relational operator that compares primitive values directly, or compares **memory addresses (references)** when used with objects.
> * `.equals()` is a method in `java.lang.Object` that can be overridden to compare the **actual content/state** of two objects (e.g., two `Vehicle` objects are considered equal if their `id` strings match).

---

### Q11: What is Autoboxing and Unboxing?
> **Answer:**
> * **Autoboxing:** Automatic conversion of a primitive data type into its corresponding wrapper class object by the compiler (e.g., `int` to `Integer`).
> * **Unboxing:** Automatic conversion of a wrapper class object back into its primitive type (e.g., `Integer` to `int`).

---

## 3. Exception Handling

### Q12: What is the difference between Checked and Unchecked Exceptions?
> **Answer:**
> * **Checked Exceptions:** Subclasses of `java.lang.Exception` (excluding `RuntimeException`). The compiler forces you to handle them using `try-catch` or declare them with `throws` (e.g., `IOException`, `FileNotFoundException` in `ReportService`).
> * **Unchecked Exceptions:** Subclasses of `java.lang.RuntimeException`. They occur during program execution due to logical programming errors (e.g., `IllegalArgumentException`, `NullPointerException`, or custom `InvalidVehicleException`). The compiler does not force you to declare them.

---

### Q13: What is the purpose of the `finally` block?
> **Answer:**
> The `finally` block always executes after the `try` and `catch` blocks, regardless of whether an exception was thrown, caught, or not thrown at all.  
> It is primarily used for cleanup tasks, such as closing open file streams (`fileWriter.close()`) or releasing database/network connections.

---

### Q14: How did you create and use a Custom Exception in your project?
> **Answer:**
> By creating a class that extends `Exception` (for checked) or `RuntimeException` (for unchecked):
> ```java
> public class InvalidVehicleException extends RuntimeException {
>     public InvalidVehicleException(String message) {
>         super(message);
>     }
> }
> ```
> We throw it using the `throw` keyword when input invariants are violated (e.g. negative speed or blank ID).

---

## 4. File Handling (I/O)

### Q15: What is the difference between Character Streams and Byte Streams in Java?
> **Answer:**
> * **Character Streams (`FileReader`, `FileWriter`, `PrintWriter`):** Process data as 16-bit Unicode characters. Ideal for reading and writing human-readable text files like our simulation report `traffic_report.txt`.
> * **Byte Streams (`FileInputStream`, `FileOutputStream`):** Process data as raw 8-bit bytes. Ideal for binary data, images, audio, or serialized object files.

---

### Q16: How do you save the simulation report to a file?
> **Answer:**
> In `ReportService.java`, we use `FileWriter` wrapped inside a `PrintWriter`:
> ```java
> try (PrintWriter writer = new PrintWriter(new FileWriter("traffic_report.txt"))) {
>     writer.println("=== Traffic Simulation Report ===");
>     writer.println("Total Vehicles Processed: " + stats.getTotalProcessed());
>     writer.println("Average Waiting Time: " + stats.getAverageWaitingTime() + " s");
> } catch (IOException e) {
>     System.out.println("Error saving report: " + e.getMessage());
> }
> ```
> Using the **try-with-resources** statement guarantees that the writer is automatically closed and flushed even if an `IOException` occurs.

---

## 5. Multithreading & Synchronization

### Q17: Why did you implement `Runnable` instead of extending `Thread` for `SimulationEngine`?
> **Answer:**
> 1. Java does not support multiple class inheritance. If `SimulationEngine` extends `Thread`, it cannot inherit from any other useful base class.
> 2. Implementing `Runnable` separates the task (the simulation logic) from the execution runner (the `Thread`), which is a cleaner design principle.

---

### Q18: What is the difference between calling `thread.start()` and `thread.run()`?
> **Answer:**
> * `thread.start()` creates a brand-new call stack and causes the JVM to run the `run()` method asynchronously on a new operating system thread.
> * `thread.run()` simply calls the method synchronously on the **current existing thread** without starting any new thread of execution.

---

### Q19: Why does the simulation need multithreading? What happens if you run it without threads?
> **Answer:**
> In a graphical Java application, the user interface runs on the **Swing Event Dispatch Thread (EDT)**.  
> If the simulation's continuous while-loop (`while(running)`) runs on the EDT, the thread becomes blocked. The GUI will freeze completely—buttons will not click, the window cannot be moved or resized, and screen repainting will halt.  
> Running `SimulationEngine` on a separate background thread allows the simulation to advance continuously while the UI remains responsive.

---

### Q20: What is a Race Condition, and how do you prevent it using `synchronized`?
> **Answer:**
> A **race condition** occurs when two or more threads concurrently access and modify shared data, leading to unpredictable or corrupted state (e.g., the UI thread adds a vehicle while the simulation thread removes one).  
> We prevent this by marking queue operations `synchronized`:
> ```java
> public synchronized void addVehicle(Vehicle v) { ... }
> public synchronized Vehicle removeVehicle() { ... }
> ```
> Only one thread can hold the road's intrinsic lock (monitor) at any given instant.

---

### Q21: How do you stop a thread safely without using deprecated methods like `thread.stop()`?
> **Answer:**
> `Thread.stop()` is deprecated because it abruptly terminates threads while they may hold locks, leaving shared data in an inconsistent state.  
> Instead, we use a **`volatile boolean running`** flag:
> ```java
> public void stopSimulation() {
>     this.running = false;
> }
> ```
> In the `run()` method, the loop periodically checks `while (running)`. When `running` becomes `false`, the loop exits gracefully, releases locks, and terminates the thread cleanly. The `volatile` keyword ensures immediate visibility across CPU caches.

---

## 6. Project-Specific Simulation Logic

### Q22: How does your emergency vehicle prioritization algorithm work?
> **Answer:**
> 1. Under normal conditions, traffic signals cycle in a round-robin schedule: North &rarr; East &rarr; South &rarr; West. Only one road is GREEN at any time; the other three are RED.
> 2. On every clock tick, the engine inspects all incoming road queues using `road.hasEmergencyVehicle()`.
> 3. If an `Ambulance` is detected on a road, normal cycling is immediately interrupted.
> 4. The road with the ambulance is switched to **GREEN**, and all other three roads are forced to **RED**.
> 5. Once the ambulance crosses the intersection, the system resumes the normal cyclic schedule.

---

### Q23: How do you prevent standard vehicles from waiting indefinitely (starvation) if multiple ambulances arrive?
> **Answer:**
> We track each vehicle's `waitingTime`. If a regular commuter vehicle's waiting time exceeds a threshold (e.g. 30 seconds), its priority increases, ensuring that normal traffic queues are flushed before accepting further non-critical preemption overrides.

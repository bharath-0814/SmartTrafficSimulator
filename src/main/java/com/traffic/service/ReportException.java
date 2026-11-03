package com.traffic.service;

/**
 * Custom checked exception thrown when an error occurs during simulation report
 * generation, file writing, or file reading.
 *
 * OOP Concepts Demonstrated:
 * 1. Custom Exceptions (Lectures 36 & 38): Extends java.lang.Exception to create a checked exception.
 * 2. Exception Chaining: Uses super(message, cause) to wrap underlying low-level IOExceptions.
 * 3. Constructor Chaining: Reuses the parent Exception class constructors.
 */
public class ReportException extends Exception {

    /**
     * Constructs a ReportException with a specified detail message.
     *
     * @param message Explanatory message for the failure.
     */
    public ReportException(String message) {
        super(message);
    }

    /**
     * Constructs a ReportException with a specified detail message and root cause.
     *
     * @param message Explanatory message for the failure.
     * @param cause   The underlying root cause exception (e.g., IOException).
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}

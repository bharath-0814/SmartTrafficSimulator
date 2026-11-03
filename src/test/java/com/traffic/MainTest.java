package com.traffic;

import com.traffic.app.Main;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic environment and sanity test verifying that JUnit 5 is working properly.
 */
class MainTest {

    @Test
    @DisplayName("Verify application metadata is present")
    void testAppMetadata() {
        assertNotNull(Main.APP_NAME, "Application name should not be null");
        assertEquals("1.0.0", Main.VERSION, "Version should match 1.0.0");
    }

    @Test
    @DisplayName("Verify main method runs without errors")
    void testMainMethod() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}

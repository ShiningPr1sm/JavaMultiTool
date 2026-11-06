package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Version comparison must be numeric-aware: plain string compare would say
 * "0.1.10" < "0.1.7" (lexicographic), which broke update checks.
 */
class UpdateManagerTest {

    private final UpdateManager updater = new UpdateManager();

    @Test
    void numericOrderBeatsLexicographic() {
        assertTrue(updater.compareVersions("0.1.10", "0.1.7") > 0, "0.1.10 must be newer than 0.1.7");
        assertTrue(updater.compareVersions("0.1.7", "0.1.10") < 0);
    }

    @Test
    void equalVersions() {
        assertEquals(0, updater.compareVersions("0.1.7", "0.1.7"));
    }

    @Test
    void missingPartsCountAsZero() {
        assertTrue(updater.compareVersions("0.2", "0.1.9") > 0);
        assertTrue(updater.compareVersions("0.1", "0.1.0") == 0);
        assertTrue(updater.compareVersions("1.0", "0.9.9.9") > 0);
    }

    @Test
    void nonNumericPartsTreatAsZero() {
        // "0.1.8-beta" should not crash the comparator
        assertTrue(updater.compareVersions("0.1.8-beta", "0.1.7") > 0);
        assertTrue(updater.compareVersions("0.1.8-beta", "0.1.8") == 0);
    }
}

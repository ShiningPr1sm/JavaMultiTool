package util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Round-trip and edge cases of the display (dd.MM.yyyy) / ISO date helpers. */
class DateUtilsTest {

    @Test
    void displayToIsoRoundTrip() {
        for (String iso : new String[]{"2026-11-06", "2024-02-29", "1999-01-01"}) {
            LocalDate d = LocalDate.parse(iso);
            assertEquals(iso, DateUtils.toISO(DateUtils.parseDisplay(DateUtils.toDisplay(d))),
                    "round trip failed for " + iso);
        }
    }

    @Test
    void toDisplayHasLeadingZeros() {
        assertEquals("06.11.2026", DateUtils.toDisplay(LocalDate.of(2026, 11, 6)));
        assertEquals("01.02.2024", DateUtils.toDisplay(LocalDate.of(2024, 2, 1)));
    }

    @Test
    void parseStrictlyRejectsInvalidDates() {
        // STRICT resolver must not silently roll invalid days/months over
        assertThrows(Exception.class, () -> DateUtils.parseDisplay("32.01.2026"));
        assertThrows(Exception.class, () -> DateUtils.parseDisplay("01.13.2026"));
        assertThrows(Exception.class, () -> DateUtils.parseDisplay("29.02.2025")); // not a leap year
    }

    @Test
    void parseAcceptsLeapDay() {
        assertEquals(LocalDate.of(2024, 2, 29), DateUtils.parseDisplay("29.02.2024"));
    }

    @Test
    void todayMatchesToday() {
        String iso = DateUtils.todayISO();
        assertEquals(LocalDate.now().toString(), iso);
        assertEquals(LocalDate.now(), DateUtils.parseDisplay(DateUtils.todayDisplay()));
    }
}

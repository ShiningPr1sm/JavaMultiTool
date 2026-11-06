package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Escaping of registry values for the .reg import file (Windows autostart). */
class ConfigManagerTest {

    @Test
    void escapesBackslashes() {
        assertEquals("C:\\\\Program Files\\\\app", ConfigManager.regEscape("C:\\Program Files\\app"));
    }

    @Test
    void escapesQuotes() {
        // Java 21: string literals read " -> \" ; reg body needs quote doubled between slashes
        assertEquals("\\\"JavaMultiTool\\\"", ConfigManager.regEscape("\"JavaMultiTool\""));
    }

    @Test
    void fullCommandEscapesBoth() {
        String command = "\"C:\\Program Files\\Zulu\\zulu-21\\bin\\javaw.exe\" -jar \"C:\\app dir\\tool.jar\"";
        String expected = "\\\"C:\\\\Program Files\\\\Zulu\\\\zulu-21\\\\bin\\\\javaw.exe\\\" -jar \\\"C:\\\\app dir\\\\tool.jar\\\"";
        assertEquals(expected, ConfigManager.regEscape(command));
    }

    @Test
    void leavesPlainPathUntouched() {
        assertEquals("plain", ConfigManager.regEscape("plain"));
        assertEquals("", ConfigManager.regEscape(""));
    }
}

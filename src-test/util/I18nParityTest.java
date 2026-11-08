package util;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EN/RU/UK bundles must share exactly the same key set. A key present in one
 * bundle but missing from another shows up in the UI as the key itself
 * (I18n logs "missing key" and falls back), so drift is a real bug.
 */
class I18nParityTest {

    private static ResourceBundle bundle(Locale locale) {
        // Locale.ROOT-based control avoids host-locale interference
        return ResourceBundle.getBundle("i18n.messages", locale, new ResourceBundle.Control() {
            @Override
            public Locale getFallbackLocale(String baseName, Locale locale) {
                return null; // no OS fallback, we compare the three bundles directly
            }
        });
    }

    @Test
    void allBundlesHaveIdenticalKeySets() {
        var en = bundle(Locale.ROOT).keySet();
        var ru = bundle(new Locale("ru")).keySet();
        var uk = bundle(new Locale("uk")).keySet();

        var ruMissing = en.stream().filter(k -> !ru.contains(k)).toList();
        var ruExtra = ru.stream().filter(k -> !en.contains(k)).toList();
        var ukMissing = en.stream().filter(k -> !uk.contains(k)).toList();
        var ukExtra = uk.stream().filter(k -> !en.contains(k)).toList();

        assertTrue(ruMissing.isEmpty() && ruExtra.isEmpty(),
                () -> "RU bundle drift. Missing in RU: " + ruMissing + ", extra in RU: " + ruExtra);
        assertTrue(ukMissing.isEmpty() && ukExtra.isEmpty(),
                () -> "UK bundle drift. Missing in UK: " + ukMissing + ", extra in UK: " + ukExtra);
    }

    @Test
    void baseBundleIsUsable() {
        var en = bundle(Locale.ROOT);
        assertTrue(en.keySet().size() > 300, "EN bundle looks unexpectedly small");
    }
}

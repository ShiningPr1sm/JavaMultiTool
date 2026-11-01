package util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Minimal i18n support. Strings live in <code>res/i18n/messages*.properties</code>
 * (Java 21 reads UTF-8 properties natively):
 * <ul>
 *   <li>{@code messages.properties} - English, base bundle, guaranteed fallback;</li>
 *   <li>{@code messages_ru.properties} - Russian;</li>
 *   <li>{@code messages_uk.properties} - Ukrainian.</li>
 * </ul>
 * Usage: {@code I18n.t("dailytracker.overview.exportJson")} or
 * {@code I18n.t("dailytracker.overview.stepsPerDay", monthTitle)} with {@code {0}} placeholders.
 */
public final class I18n {

    private static final String BASE_NAME = "i18n.messages";

    /** Display names per language code (native spelling - these are never translated). */
    private static final String[] CODES = {"en", "ru", "uk"};
    private static final String[] DISPLAY = {"English", "Русский", "Українська"};

    private static volatile ResourceBundle bundle =
            ResourceBundle.getBundle(BASE_NAME, Locale.ENGLISH);
    private static volatile Locale locale = Locale.ENGLISH;

    private I18n() {}

    /** Sets the language ("en", "ru", "uk"); anything unknown falls back to English. */
    public static void setLanguage(String code) {
        Locale newLocale = switch (normalize(code)) {
            case "ru" -> Locale.forLanguageTag("ru");
            case "uk" -> Locale.forLanguageTag("uk");
            default -> Locale.ENGLISH;
        };
        if (newLocale.equals(locale)) return;
        locale = newLocale;
        bundle = ResourceBundle.getBundle(BASE_NAME, newLocale);
        AppLogger.info("I18n: language set to " + locale.toLanguageTag());
    }

    /** Translates a key; unknown keys are logged and returned as-is (visible while developing). */
    public static String t(String key, Object... args) {
        try {
            String text = bundle.getString(key);
            if (args == null || args.length == 0) return text;
            return MessageFormat.format(text, args);
        } catch (Exception e) {
            AppLogger.error("I18n: missing key '" + key + "'");
            return key;
        }
    }

    /** Current language code ("en", "ru", "uk"). */
    public static String currentLanguage() {
        String tag = locale.toLanguageTag();
        return tag.startsWith("ru") ? "ru" : tag.startsWith("uk") ? "uk" : "en";
    }

    /** Locale for Collators, date/number formatting. */
    public static Locale locale() {
        return locale;
    }

    private static String normalize(String code) {
        if (code == null || code.isBlank()) return "en";
        String c = code.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "english", "en" -> "en";
            case "русский", "ru" -> "ru";
            case "українська", "uk" -> "uk";
            default -> c;
        };
    }

    /** Display name ("English" / "Русский" / "Українська") for a language code. */
    public static String displayName(String code) {
        String norm = normalize(code);
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(norm)) return DISPLAY[i];
        }
        return DISPLAY[0];
    }

    /** All display names (combo box items, combo index == index in {@link #codeOf}). */
    public static String[] displayNames() {
        return DISPLAY.clone();
    }

    /** Language code from a display name (or raw code) - inverse of {@link #displayName(String)}. */
    public static String codeOf(String displayNameOrCode) {
        return normalize(displayNameOrCode);
    }
}

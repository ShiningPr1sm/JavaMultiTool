package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class DateUtils {
    public static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withResolverStyle(ResolverStyle.STRICT);

    private DateUtils() {
    }

    public static String toDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String toISO(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate parseDisplay(String text) {
        return LocalDate.parse(text, DISPLAY_FORMAT);
    }

    public static String todayDisplay() {
        return toDisplay(LocalDate.now());
    }

    public static String todayISO() {
        return toISO(LocalDate.now());
    }
}
package ui.search;

import java.util.List;

/**
 * Static catalog of everything the search can find: one entry per feature
 * reachable from the sidebar, header or settings. Tags are short visible
 * keywords (rendered as badges), hidden words power the "you may be looking
 * for" section - they contain words that do NOT appear in the title
 * (e.g. typing "replace" surfaces Settings and Find & Replace).
 */
public final class SearchIndex {
    private SearchIndex() {
    }

    /** @param tester true shows tester-only entries (Admin Panel). */
    public static List<SearchEntry> entries(boolean tester) {
        return List.of(
                new SearchEntry("main.welcome", null,
                        new String[]{"home"},
                        new String[]{"start", "main", "старт", "домой"},
                        "Welcome"),
                new SearchEntry("main.achievements", null,
                        new String[]{"level", "xp"},
                        new String[]{"progress", "reward", "badge", "прогресс", "награда", "уровень"},
                        "Achievements"),
                new SearchEntry("sidebar.notifications", null,
                        new String[]{"alerts", "reminders"},
                        new String[]{"notification", "уведомления", "оповещения", "напоминания"},
                        "sidebar.notifications"),
                new SearchEntry("main.settings", null,
                        new String[]{"language", "theme", "appearance"},
                        new String[]{"change", "skin", "config", "change", "language", "theme", "appearance", "tuning", "skin"},
                        "Settings"),

                new SearchEntry("sidebar.mediaDownloader", "sidebar.photoVideo",
                        new String[]{"video", "download", "media", "yt"},
                        new String[]{"youtube", "yt-dlp", "download", "download", "media", "video"},
                        "sidebar.mediaDownloader"),
                new SearchEntry("sidebar.fileOrganizer", "sidebar.photoVideo",
                        new String[]{"files", "sort", "organize"},
                        new String[]{"cleanup", "sorting", "folders", "files", "cleaning"},
                        "sidebar.fileOrganizer"),
                new SearchEntry("sidebar.imageTools", "sidebar.photoVideo",
                        new String[]{"photo", "resize", "compress"},
                        new String[]{"resize", "crop", "image", "compression", "trim", "metadata", "exif"},
                        "sidebar.imageTools"),

                new SearchEntry("sidebar.unitConverter", "sidebar.math",
                        new String[]{"units", "convert", "calc"},
                        new String[]{"conversion", "convert", "units", "length", "weight", "temperature", "length", "weight", "temperature"},
                        "sidebar.unitConverter"),

                new SearchEntry("sidebar.findReplace", "sidebar.text",
                        new String[]{"find", "replace", "text"},
                        new String[]{"replace", "replace", "replace", "search text", "find"},
                        "sidebar.findReplace"),

                new SearchEntry("sidebar.workflow", "sidebar.time",
                        new String[]{"pomodoro", "timer", "apps"},
                        new String[]{"pomodoro", "timer", "process", "tracking", "timer", "pomodoro", "processes"},
                        "sidebar.workflow"),
                new SearchEntry("sidebar.birthdayTracker", "sidebar.time",
                        new String[]{"bdays", "calendar", "reminders"},
                        new String[]{"birthday", "days until", "reminder", "birthday", "birthdays", "reminder"},
                        "sidebar.birthdayTracker"),
                new SearchEntry("sidebar.dailyTracker", "sidebar.time",
                        new String[]{"calories", "steps", "purchases"},
                        new String[]{"calories", "steps", "nutrition", "money spent", "calories", "steps", "purchases", "nutrition"},
                        "sidebar.dailyTracker"),

                new SearchEntry("sidebar.colorPicker", "sidebar.utils",
                        new String[]{"color", "hex", "rgb"},
                        new String[]{"eyedropper", "palette", "eyedropper", "color", "palette"},
                        "sidebar.colorPicker"),
                new SearchEntry("sidebar.passwordGenerator", "sidebar.utils",
                        new String[]{"password", "secure", "random"},
                        new String[]{"pass", "generator", "secure", "password", "random", "secure"},
                        "sidebar.passwordGenerator"),
                new SearchEntry("sidebar.qrGenerator", "sidebar.utils",
                        new String[]{"qr", "code", "scan"},
                        new String[]{"qr", "barcode", "scanner", "code"},
                        "sidebar.qrGenerator"),
                new SearchEntry("sidebar.networkTools", "sidebar.utils",
                        new String[]{"network", "ping", "ip"},
                        new String[]{"network", "port", "ping", "connection", "speed", "network", "connection"},
                        "sidebar.networkTools"),

                new SearchEntry("sidebar.adminCmd", "sidebar.adminPanel",
                        new String[]{"admin", "log", "tester"},
                        new String[]{"cmd", "console", "logs", "console", "admin"},
                        "sidebar.adminCmd")
        ).stream().filter(e -> !"sidebar.adminPanel".equals(e.pathKey()) || tester).toList();
    }
}
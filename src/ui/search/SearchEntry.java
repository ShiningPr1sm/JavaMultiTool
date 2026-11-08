package ui.search;

/**
 * One searchable feature of the app.
 *
 * @param titleKey i18n key of the displayed title (identity key, translated at render)
 * @param pathKey  i18n key of the parent section shown on the right ("Photo & Video").
 *                 Null for app-level pages (Welcome, Settings...) - they show no path.
 * @param tags     visible tags rendered as colored badges, e.g. {"video", "download"}
 * @param hidden   invisible keywords matched by a separate "you may be looking for"
 *                 section, e.g. typing "replace" finds Find & Replace
 * @param targetKey navigation target: a "sidebar.*" openTab key ("Settings" is a
 *                  valid openTab key too) or "Welcome"/"Achievements"
 */
public record SearchEntry(String titleKey, String pathKey, String[] tags, String[] hidden, String targetKey) {

    String title() {
        return util.I18n.t(titleKey);
    }
}

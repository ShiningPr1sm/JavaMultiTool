package util;

public class MarkdownUtil {
    public static String toPlainText(String markdown) {
        if (markdown == null) return "(no release notes)";
        String result = markdown
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("(?m)^[-*]\\s+", "• ")
                .trim();
        return result.isEmpty() ? "(no release notes)" : result;
    }
}

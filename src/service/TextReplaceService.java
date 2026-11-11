package service;

import util.AppLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Search/replace engine for the Find &amp; Replace tool: sequential rule
 * application, batch rewriting of a directory tree and identifier case styles.
 */
public class TextReplaceService {

    private static final int FLAGS = Pattern.MULTILINE;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Pattern IDENT_RUN = Pattern.compile("[\\p{L}\\d]+(?:[_\\-][\\p{L}\\d]+)*");
    private static final Pattern CAMEL_SPLIT =
            Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

    /** One find(regex) -> replace rule from the rules table. */
    public record Rule(String find, String replacement) {}

    /** Directory batch outcome counts. */
    public record BatchResult(int scanned, int changed, int failed) {}

    public enum CaseStyle { CAMEL, SNAKE, KEBAB, UPPER, LOWER }

    private static int flags(boolean matchCase) {
        return matchCase ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    }

    /** Single find & replace pass: literal or regex search, optional case folding. */
    public String applySingle(String text, String find, String replacement,
                              boolean useRegex, boolean matchCase) {
        if (!useRegex) {
            return Pattern.compile(find, Pattern.LITERAL | flags(matchCase)).matcher(text)
                    .replaceAll(Matcher.quoteReplacement(replacement));
        }
        return Pattern.compile(find, FLAGS | flags(matchCase)).matcher(text)
                .replaceAll(replacement);
    }

    /** All matches of one single-mode search (group 1 when present), joined by newline. */
    public String collectMatches(String text, String find, boolean useRegex, boolean matchCase) {
        Pattern pattern = useRegex
                ? Pattern.compile(find, FLAGS | flags(matchCase))
                : Pattern.compile(find, Pattern.LITERAL | flags(matchCase));
        Matcher m = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hit = m.groupCount() > 0 && m.group(1) != null ? m.group(1) : m.group();
            if (!hit.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(hit);
            }
        }
        return sb.toString();
    }

    /** Applies every rule sequentially; rules with a broken regex are skipped
     *  with an info log instead of aborting the whole pass. */
    public String applyRules(String text, List<Rule> rules) {
        String result = text;
        for (Rule rule : rules) {
            try {
                result = Pattern.compile(rule.find(), FLAGS).matcher(result)
                        .replaceAll(rule.replacement());
            } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
                AppLogger.info("Skipping invalid rule '" + rule.find() + "': " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Rewrites every file with a matching extension under {@code root}
     * (recursively). Files larger than 20 MB or non-UTF-8 content are counted
     * as failures and left untouched.
     */
    public BatchResult processDirectory(File root, List<String> extensions, List<Rule> rules,
                                        boolean backup) throws IOException {
        int scanned = 0;
        int changed = 0;
        int failed = 0;

        try (Stream<Path> stream = Files.walk(root.toPath())) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesExtension(p.getFileName().toString(), extensions))
                    .toList();

            for (Path path : files) {
                scanned++;
                try {
                    String original = Files.readString(path);
                    String processed = applyRules(original, rules);
                    if (processed.equals(original)) continue;
                    if (backup) {
                        Files.copy(path, path.resolveSibling(
                                path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.writeString(path, processed);
                    changed++;
                } catch (IOException e) {
                    failed++;
                    AppLogger.info("Skipped file " + path + ": " + e.getMessage());
                }
            }
        }
        return new BatchResult(scanned, changed, failed);
    }

    /**
     * Converts identifier-like runs ("user_profile", "userProfile", "user-profile")
     * between styles while keeping the surrounding text, spaces and punctuation.
     */
    public String transform(String text, CaseStyle style) {
        if (style == CaseStyle.UPPER) return text.toUpperCase(Locale.ROOT);
        if (style == CaseStyle.LOWER) return text.toLowerCase(Locale.ROOT);

        Matcher m = IDENT_RUN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            List<String> words = new ArrayList<>();
            for (String part : m.group().split("[_\\-]+")) {
                if (!part.isEmpty()) {
                    for (String w : CAMEL_SPLIT.split(part)) words.add(w);
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(joinWords(words, style)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String joinWords(List<String> words, CaseStyle style) {
        if (words.isEmpty()) return "";
        if (style == CaseStyle.CAMEL) {
            StringBuilder sb = new StringBuilder(words.get(0).toLowerCase(Locale.ROOT));
            for (int i = 1; i < words.size(); i++) {
                String w = words.get(i).toLowerCase(Locale.ROOT);
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
            return sb.toString();
        }
        String joiner = style == CaseStyle.SNAKE ? "_" : "-";
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(joiner);
            sb.append(w.toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static boolean matchesExtension(String fileName, List<String> extensions) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : extensions) {
            if (lower.endsWith("." + ext)) return true;
        }
        return false;
    }
}

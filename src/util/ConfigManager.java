package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {
    private static final Path CONFIG_PATH = Path.of(AppPaths.CONFIG_FILE);

    public static String loadSkippedVersion() {
        return load().getProperty("skippedVersion", "");
    }

    public static void saveSkippedVersion(String version) {
        Properties props = load();
        props.setProperty("skippedVersion", version);
        save(props);
    }

    public static String loadProperty(String key, String defaultValue) {
        return load().getProperty(key, defaultValue);
    }

    public static boolean isInternetEnabled() {
        return Boolean.parseBoolean(loadProperty("useInternet", "true"));
    }

    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_REG_NAME = "JavaMultiTool";

    public static boolean isStartupEnabled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", REG_KEY, "/v", APP_REG_NAME);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            return p.waitFor() == 0 && output.contains(APP_REG_NAME);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Toggles autostart with Windows (HKCU CurrentVersion\\Run).
     * Written via a temp .reg file + "reg import": passing a value with embedded
     * quotes ("javaw -jar \\"...\\"") through ProcessBuilder breaks reg.exe with
     * "Invalid syntax" (Java does not escape inner quotes on Windows), while the
     * import-file route has no command-line quoting pitfalls.
     *
     * @param enabled true to register the jar in the Run key, false to remove it
     */
    public static void setStartup(boolean enabled) {
        try {
            if (enabled) {
                String jarPath = getJarPath();
                if (jarPath == null) {
                    AppLogger.error("ConfigManager: cannot determine JAR path for startup");
                    return;
                }
                String javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
                Path script = Files.createTempFile("javamultitool_startup_", ".reg");
                // Value shown to Windows later: "C:\...\javaw.exe" -jar "C:\...\app.jar"
                String command = "\"" + javaw + "\" -jar \"" + jarPath + "\"";
                Files.writeString(script, String.format(
                        "Windows Registry Editor Version 5.00%n%n"
                        + "[HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run]%n"
                        + "\"%s\"=\"%s\"%n",
                        APP_REG_NAME, regEscape(command)));
                ProcessBuilder pb = new ProcessBuilder("reg", "import", script.toString());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes());
                boolean ok = p.waitFor() == 0;
                Files.deleteIfExists(script);
                if (ok && isStartupEnabled()) {
                    AppLogger.info("ConfigManager: startup enabled, jar=" + jarPath);
                } else {
                    AppLogger.error("ConfigManager: startup import failed - " + output.trim());
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder("reg", "delete", REG_KEY, "/v", APP_REG_NAME, "/f");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
                AppLogger.info("ConfigManager: startup disabled, exit=" + p.exitValue());
            }
        } catch (Exception e) {
            AppLogger.error("ConfigManager: failed to set startup: " + e.getMessage());
        }
    }

    /** Escapes a string for a value body in a .reg file: backslash -> double, quote -> \\\". */
    private static String regEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String getJarPath() {
        try {
            Path javaHome = Path.of(System.getProperty("java.home"));
            Path appDir = javaHome.getParent();
            if (appDir != null) {
                Path cfgPath = appDir.resolve("JMT.cfg");
                if (Files.exists(cfgPath)) {
                    for (String line : Files.readAllLines(cfgPath)) {
                        line = line.trim();
                        if (line.startsWith("app.classpath=")) {
                            String cp = line.substring("app.classpath=".length()).trim();
                            cp = cp.replace("$APPDIR", appDir.toString());
                            if (Files.exists(Path.of(cp))) return cp;
                        }
                    }
                }
            }
            String javaClassPath = System.getProperty("java.class.path");
            if (javaClassPath != null && !javaClassPath.isEmpty()) {
                File f = new File(javaClassPath);
                if (f.isFile() && javaClassPath.endsWith(".jar")) {
                    return f.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            AppLogger.error("ConfigManager: cannot determine JAR path: " + e.getMessage());
        }
        return null;
    }

    public static void saveProperty(String key, String value) {
        Properties props = load();
        props.setProperty(key, value);
        save(props);
    }

    private static Properties load() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            } catch (IOException e) {
                AppLogger.error("ConfigManager: failed to load config: " + e.getMessage());
            }
        }
        return props;
    }

    private static void save(Properties props) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "JavaMultiTool preferences");
            }
        } catch (IOException e) {
            AppLogger.error("ConfigManager: failed to save config: " + e.getMessage());
        }
    }
}

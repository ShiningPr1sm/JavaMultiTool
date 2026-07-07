package util;

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

package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AppPaths {
    private static final String ROAMING = System.getenv("APPDATA") != null
            ? System.getenv("APPDATA")
            : System.getProperty("user.home");
    private static final Path COMMON_PATH = Path.of(ROAMING, "ShiningPr1sm");
    private static final Path APP_PATH = COMMON_PATH.resolve("JavaMultiTool");

    /** App data folder (read-only use by e.g. SingleInstanceGuard). */
    public static String appDir() {
        return APP_PATH.toString();
    }

    public static final String COMMON_DIR = COMMON_PATH.toString();
    public static final String LOGS_DIR = APP_PATH.resolve("logs").toString();
    public static final String RIGHTS_FILE = APP_PATH.resolve("rights.ini").toString();
    public static final String CONFIG_FILE = APP_PATH.resolve("update.properties").toString();

    private static Path currentUserRoot;

    public static String DB_USER = APP_PATH.resolve("user_data.db").toString();
    public static String DB_WORKFLOW = APP_PATH.resolve("Databases").resolve("workflow.db").toString();
    public static String DB_ACHIEVEMENTS = APP_PATH.resolve("Databases").resolve("achievements.db").toString();
    public static String DB_BDAYS = APP_PATH.resolve("Databases").resolve("bdays.db").toString();
    public static String DB_LEVELS = APP_PATH.resolve("Databases").resolve("levels.db").toString();
    public static String DB_DAILY = APP_PATH.resolve("Databases").resolve("daily.db").toString();

    public static void init() {
        Path databaseDir = APP_PATH.resolve("Databases");
        databaseDir.toFile().mkdirs();
        Path legacyUserDb = APP_PATH.resolve("user_data.db");
        DB_USER = legacyUserDb.toFile().exists()
                ? legacyUserDb.toString()
                : databaseDir.resolve("user_data.db").toString();
    }

    public static String avatarFile(String login) {
        return currentUserRoot != null ? currentUserRoot.resolve(login + ".png").toString() : null;
    }

    public static void setUserRoot(String nickname) {
        currentUserRoot = APP_PATH.resolve(nickname);
        Path userDbDir = currentUserRoot.resolve("Databases");
        userDbDir.toFile().mkdirs();
        DB_WORKFLOW = userDbDir.resolve("workflow.db").toString();
        DB_ACHIEVEMENTS = userDbDir.resolve("achievements.db").toString();
        DB_BDAYS = userDbDir.resolve("bdays.db").toString();
        DB_LEVELS = userDbDir.resolve("levels.db").toString();
        DB_DAILY = userDbDir.resolve("daily.db").toString();
    }

    public static void migrateUserData(String login, String nickname) {
        Path oldDbDir = APP_PATH.resolve("Databases");
        Path oldAvatarsDir = APP_PATH.resolve("avatars");
        Path userRoot = APP_PATH.resolve(nickname);
        boolean needsMigration = !userRoot.toFile().exists();

        setUserRoot(nickname);
        if (!needsMigration) return;

        try {
            Path oldUserDb = oldDbDir.resolve("user_data.db");
            if (oldUserDb.toFile().exists()) {
                Files.move(oldUserDb, APP_PATH.resolve("user_data.db"), StandardCopyOption.REPLACE_EXISTING);
            }
            DB_USER = APP_PATH.resolve("user_data.db").toString();

            Path userDbDir = userRoot.resolve("Databases");
            copyIfExists(oldDbDir.resolve("workflow.db"), userDbDir.resolve("workflow.db"));
            copyIfExists(oldDbDir.resolve("achievements.db"), userDbDir.resolve("achievements.db"));
            copyIfExists(oldDbDir.resolve("bdays.db"), userDbDir.resolve("bdays.db"));
            copyIfExists(oldDbDir.resolve("levels.db"), userDbDir.resolve("levels.db"));

            Path sharedAvatar = oldAvatarsDir.resolve(login + ".png");
            if (sharedAvatar.toFile().exists()) {
                Files.move(sharedAvatar, userRoot.resolve(login + ".png"), StandardCopyOption.REPLACE_EXISTING);
            }

            deleteDir(oldDbDir);
            deleteDir(oldAvatarsDir);
        } catch (IOException e) {
            AppLogger.error("AppPaths: migration failed - " + e.getMessage());
        }
    }

    private static void copyIfExists(Path from, Path to) throws IOException {
        if (from.toFile().exists()) {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteDir(Path path) {
        File dir = path.toFile();
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        dir.delete();
    }
}

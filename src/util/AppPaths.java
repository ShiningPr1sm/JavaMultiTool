package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AppPaths {
    private static final String ROAMING = System.getenv("APPDATA");
    private static final String SHINING_PR1SM = ROAMING + "/ShiningPr1sm";
    private static final String APP_DIR = SHINING_PR1SM + "/JavaMultiTool";

    public static final String COMMON_DIR = SHINING_PR1SM;
    public static final String LOG_FILE = APP_DIR + "/app_history.log";
    public static final String RIGHTS_FILE = APP_DIR + "/rights.ini";
    public static final String CONFIG_FILE = APP_DIR + "/update.properties";

    private static String currentUserRoot;

    public static String DB_USER = APP_DIR + "/user_data.db";
    public static String DB_WORKFLOW = APP_DIR + "/Databases/workflow.db";
    public static String DB_ACHIEVEMENTS = APP_DIR + "/Databases/achievements.db";
    public static String DB_BDAYS = APP_DIR + "/Databases/bdays.db";
    public static String DB_LEVELS = APP_DIR + "/Databases/levels.db";

    public static void init() {
        new File(APP_DIR + "/Databases").mkdirs();
        DB_USER = new File(APP_DIR + "/Databases/user_data.db").exists()
            ? APP_DIR + "/Databases/user_data.db"
            : APP_DIR + "/user_data.db";
    }

    public static String avatarFile(String login) {
        return currentUserRoot != null ? currentUserRoot + "/" + login + ".png" : null;
    }

    public static void setUserRoot(String nickname) {
        currentUserRoot = APP_DIR + "/" + nickname;
        new File(currentUserRoot + "/Databases").mkdirs();
        String userDbDir = currentUserRoot + "/Databases";
        DB_WORKFLOW = userDbDir + "/workflow.db";
        DB_ACHIEVEMENTS = userDbDir + "/achievements.db";
        DB_BDAYS = userDbDir + "/bdays.db";
        DB_LEVELS = userDbDir + "/levels.db";
    }

    public static void migrateUserData(String login, String nickname) {
        String oldDbDir = APP_DIR + "/Databases";
        String oldAvatarsDir = APP_DIR + "/avatars";
        String userRoot = APP_DIR + "/" + nickname;
        boolean needsMigration = !new File(userRoot).exists();

        setUserRoot(nickname);
        if (!needsMigration) return;

        try {
            File oldUserDb = new File(oldDbDir + "/user_data.db");
            if (oldUserDb.exists()) {
                Files.move(oldUserDb.toPath(), new File(APP_DIR + "/user_data.db").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            DB_USER = APP_DIR + "/user_data.db";

            copyIfExists(oldDbDir + "/workflow.db", userRoot + "/Databases/workflow.db");
            copyIfExists(oldDbDir + "/achievements.db", userRoot + "/Databases/achievements.db");
            copyIfExists(oldDbDir + "/bdays.db", userRoot + "/Databases/bdays.db");
            copyIfExists(oldDbDir + "/levels.db", userRoot + "/Databases/levels.db");

            File sharedAvatar = new File(oldAvatarsDir + "/" + login + ".png");
            if (sharedAvatar.exists()) {
                Files.move(sharedAvatar.toPath(), new File(userRoot + "/" + login + ".png").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            deleteDir(oldDbDir);
            deleteDir(oldAvatarsDir);
        } catch (IOException e) {
            AppLogger.error("AppPaths: migration failed - " + e.getMessage());
        }
    }

    private static void copyIfExists(String from, String to) throws IOException {
        File src = new File(from);
        if (src.exists()) {
            Files.copy(src.toPath(), new File(to).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        dir.delete();
    }
}

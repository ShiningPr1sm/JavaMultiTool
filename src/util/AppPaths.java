package util;

import java.io.File;

public class AppPaths {
    private static final String ROAMING = System.getenv("APPDATA");
    private static final String SHINING_PR1SM = ROAMING + "/ShiningPr1sm";
    private static final String APP_DIR = SHINING_PR1SM + "/JavaMultiTool";
    private static final String DB_DIR = APP_DIR + "/Databases";
    private static final String AVATARS_DIR = APP_DIR + "/avatars";

    public static final String COMMON_DIR = SHINING_PR1SM;
    public static final String DB_USER = DB_DIR + "/user_data.db";
    public static final String DB_WORKFLOW = DB_DIR + "/workflow.db";
    public static final String DB_ACHIEVEMENTS = DB_DIR + "/achievements.db";
    public static final String DB_BDAYS = DB_DIR + "/bdays.db";
    public static final String DB_LEVELS = DB_DIR + "/levels.db";
    public static final String LOG_FILE = APP_DIR + "/app_history.log";
    public static final String RIGHTS_FILE = APP_DIR + "/rights.ini";
    public static final String AVATAR_DIR = AVATARS_DIR;
    public static final String CONFIG_FILE = APP_DIR + "/update.properties";

    public static void init() {
        new File(DB_DIR).mkdirs();
        new File(AVATARS_DIR).mkdirs();
    }

    public static String avatarFile(String login) {
        return AVATARS_DIR + "/" + login + ".png";
    }
}

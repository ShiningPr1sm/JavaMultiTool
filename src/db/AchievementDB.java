package db;

import ui.MainFrame;
import ui.utils.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AchievementDB {
    private static final String DB_PATH = System.getProperty("user.home") + "/Documents/MultiTool-Java-data/Databases/achievements.db";
    private static MainFrame mainFrame;

    public static void setMainFrame(MainFrame frame) {
        mainFrame = frame;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Основная таблица метаданных ачивок
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT UNIQUE NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL
                );
            """);
            // Таблица уровней для каждой ачивки
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS achievement_levels (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    achievement_code TEXT NOT NULL,
                    level INTEGER NOT NULL,
                    required_progress INTEGER NOT NULL,
                    xp_reward INTEGER NOT NULL,
                    UNIQUE(achievement_code, level),
                    FOREIGN KEY(achievement_code) REFERENCES achievements(code)
                );
            """);
            // Таблица состояния достижения пользователя
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_achievements (
                    user_login TEXT NOT NULL,
                    achievement_code TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    progress INTEGER NOT NULL DEFAULT 0,
                    unlocked_at TEXT,
                    PRIMARY KEY(user_login, achievement_code),
                    FOREIGN KEY(achievement_code) REFERENCES achievements(code)
                );
            """);
        } catch (SQLException e) {
            AppLogger.log("Achievements initializeDatabase DB", e.getMessage());
        }
    }

    /**
     * Вставляет метаданные ачивки без уровней.
     */
    public static void addAchievement(String code, String title, String description) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO achievements(code, title, description) VALUES(?, ?, ?)")
        ) {
            stmt.setString(1, code);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("AchievementDB.addAchievement (" + code + ") failed: " + e.getMessage());
        }
    }

    /**
     * Добавляет уровень для существующей ачивки.
     */
    public static void addAchievementLevel(String code, int level, int requiredProgress, int xpReward) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO achievement_levels(achievement_code, level, required_progress, xp_reward) VALUES(?, ?, ?, ?)")
        ) {
            stmt.setString(1, code);
            stmt.setInt(2, level);
            stmt.setInt(3, requiredProgress);
            stmt.setInt(4, xpReward);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.log("Achievements DB", e.getMessage());
        }
    }

    /**
     * Выборка ачивки по её коду и возвращает максимальный уровень ачивки.
     */
    public static int getMaxAchievementLevel(String code) {
        String sql = "SELECT MAX(level) FROM achievement_levels WHERE achievement_code = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Возвращает кол-во всех лвл-в ачивок.
     */
    public static int getTotalAchievementsLevels() {
        String sql = "SELECT COUNT(*) FROM achievement_levels";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Берет все выполненные ачивки пользователя и возвращает число.
     */
    public static int getTotalUserAchievementsLevels(String login) {
        String sql = "SELECT COUNT(*) FROM user_achievements WHERE user_login = ? AND level > 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Заполняет базу стандартными ачивками и их первым уровнем.
     */
    public static void insertDefaultAchievements() {
        /*
        level: 1 - ачивка на 1 раз и выполнена.
        level: 2 - ачивка на 2 раз чтобы была выполнена.
        level: n - ачивка на n раз чтобы была выполнена.
         */

        addAchievement("first_login", "I am new here!", "Log into the application for the first time.");
        addAchievementLevel("first_login", 1, 1, 20);

        addAchievement("change_nickname", "New name - new Me!", "Change your nickname.");
        addAchievementLevel("change_nickname", 1, 1, 30);

        addAchievement("reach_10lvl", "King of XP", "Reach lvl 10.");
        addAchievementLevel("reach_10lvl", 1, 1, 100);

        addAchievement("first_steps", "Now I know all", "Add your first note.");
        addAchievementLevel("first_steps", 1, 1, 20);

        addAchievement("timer_sec", "Dr. Stephen Vincent Strange", "Start a timer.");
        addAchievementLevel("timer_sec", 1, 1, 20);

        addAchievement("real_friend", "You are a real friend..", "Add your first date to BDays notifier.");
        addAchievementLevel("real_friend", 1, 1, 25);
    }

    /**
     * Синхронизирует табличку user_achievements: добавляет новые записи для пользователя.
     */
    public static void syncUserAchievements(String login) {
        try (Connection conn = getConnection();
             PreparedStatement getAll = conn.prepareStatement(
                     "SELECT code FROM achievements");
             ResultSet rs = getAll.executeQuery()
        ) {
            while (rs.next()) {
                String code = rs.getString("code");
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM user_achievements WHERE user_login=? AND achievement_code=?");
                     PreparedStatement insert = conn.prepareStatement(
                             "INSERT INTO user_achievements(user_login, achievement_code, level, progress) VALUES(?, ?, 1, 0)")
                ) {
                    check.setString(1, login);
                    check.setString(2, code);
                    if (!check.executeQuery().next()) {
                        insert.setString(1, login);
                        insert.setString(2, code);
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Возвращает прогресс текущего уровня для пользователя.
     */
    public static Map<String, Integer> getUserProgress(String login) {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT achievement_code, progress FROM user_achievements WHERE user_login=?")
        ) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("achievement_code"), rs.getInt("progress"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Обработка завершения шага прогресса: добавление прогресса и возможное повышение уровня.
     */
    public static void completeAchievement(String login, String code) {
        try (Connection conn = getConnection()) {
            // Получаем текущее состояние
            PreparedStatement getState = conn.prepareStatement(
                    "SELECT level, progress FROM user_achievements WHERE user_login=? AND achievement_code=?");
            getState.setString(1, login);
            getState.setString(2, code);
            ResultSet rs = getState.executeQuery();
            if (!rs.next()) return;

            int level = rs.getInt("level");
            int progress = rs.getInt("progress") + 1; // +1 шаг

            PreparedStatement getLevel = conn.prepareStatement(
                    "SELECT required_progress, xp_reward FROM achievement_levels WHERE achievement_code=? AND level=?");
            getLevel.setString(1, code);
            getLevel.setInt(2, level);
            ResultSet lvlRs = getLevel.executeQuery();
            if (!lvlRs.next()) return;

            int required = lvlRs.getInt("required_progress");
            int reward  = lvlRs.getInt("xp_reward");
            boolean leveledUp = false;

            if (progress >= required) {
                progress -= required;
                level++;
                leveledUp = true;
            }
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE user_achievements SET level=?, progress=?, unlocked_at=? " +
                            "WHERE user_login=? AND achievement_code=?");
            update.setInt(1, level);
            update.setInt(2, progress);
            update.setString(3, leveledUp ? LocalDateTime.now().toString() : null);
            update.setString(4, login);
            update.setString(5, code);
            update.executeUpdate();

            if (leveledUp) {
                mainFrame.grantXP(login, reward);
                if (mainFrame != null) mainFrame.demoGrantXP(reward);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

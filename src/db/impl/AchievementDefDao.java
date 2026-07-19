package db.impl;

import util.AppLogger;

import java.sql.*;

public class AchievementDefDao {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_ACHIEVEMENTS);
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT UNIQUE NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL
                );
            """);
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

    public void addAchievement(String code, String title, String description) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO achievements(code, title, description) VALUES(?, ?, ?)")
        ) {
            stmt.setString(1, code);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("addAchievement (" + code + ") failed: " + e.getMessage());
        }
    }

    public void addAchievementLevel(String code, int level, int requiredProgress, int xpReward) {
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

    public int getMaxAchievementLevel(String code) {
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
            AppLogger.error("AchievementDefDao SQL error: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalAchievementsLevels() {
        String sql = "SELECT COUNT(*) FROM achievement_levels";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("AchievementDefDao SQL error: " + e.getMessage());
        }
        return 0;
    }

    public void insertDefaultAchievements() {
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

        addAchievement("real_friend", "You are a real friend..", "Add your first date to Birthday Tracker.");
        addAchievementLevel("real_friend", 1, 1, 25);

        addAchievement("security", "Safety is everything to us!", "Generate a password and then copy it.");
        addAchievementLevel("security", 1, 1, 15);

        addAchievement("qrcode", "QRcoder", "Generate and download the first QR code.");
        addAchievementLevel("qrcode", 1, 1, 20);

        addAchievement("magnifier", "What are you looking for here??", "View the photo's metadata for the first time.");
        addAchievementLevel("magnifier", 1, 1, 10);
    }
}

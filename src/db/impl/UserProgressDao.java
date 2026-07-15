package db.impl;

import util.AppLogger;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserProgressDao {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_ACHIEVEMENTS);
    }

    public int getTotalUserAchievementsLevels(String login) {
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
            AppLogger.error("UserProgressDao SQL error: " + e.getMessage());
        }
        return 0;
    }

    public void syncUserAchievements(String login) {
        try (Connection conn = getConnection();
             PreparedStatement getAll = conn.prepareStatement("SELECT code FROM achievements");
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
            AppLogger.error("UserProgressDao SQL error: " + e.getMessage());
        }
    }

    public Map<String, Integer> getUserProgress(String login) {
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
            AppLogger.error("UserProgressDao SQL error: " + e.getMessage());
        }
        return map;
    }
}

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

package service;

import db.AchievementRepository;
import db.DatabaseProvider;
import util.AppLogger;
import util.AchievementCallback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AchievementService {

    public static class AchievementData {
        public final String code;
        public final String title;
        public final String description;
        public final int xpReward;
        public final int level;
        public final int progress;
        public final int required;
        public final int maxLevel;

        public AchievementData(String code, String title, String description,
                               int xpReward, int level, int progress, int required, int maxLevel) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.xpReward = xpReward;
            this.level = level;
            this.progress = progress;
            this.required = required;
            this.maxLevel = maxLevel;
        }

        public String getDisplayTitle() {
            return maxLevel > 1 ? title + " (Level " + level + ")" : title;
        }

        public boolean isCompleted() {
            return progress >= required;
        }
    }

    public List<AchievementData> loadUserAchievements(String login) {
        List<AchievementData> result = new ArrayList<>();
        String sql = """
            SELECT a.code,
                   a.title,
                   a.description,
                   al.xp_reward AS xp_reward,
                   COALESCE(ua.level, 1) AS level,
                   COALESCE(ua.progress, 0) AS progress,
                   al.required_progress
              FROM achievements a
              LEFT JOIN user_achievements ua
                ON a.code = ua.achievement_code
               AND ua.user_login = ?
              LEFT JOIN achievement_levels al
                ON a.code = al.achievement_code
               AND COALESCE(ua.level, 1) = al.level
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    int xpReward = rs.getInt("xp_reward");
                    int level = rs.getInt("level");
                    Integer reqObj = (Integer) rs.getObject("required_progress");

                    int required;
                    int progress;
                    if (reqObj == null) {
                        required = 1;
                        progress = 1;
                    } else {
                        required = reqObj;
                        progress = rs.getInt("progress");
                    }

                    int maxLevel = repo().getMaxAchievementLevel(code);
                    result.add(new AchievementData(code, title, description, xpReward, level, progress, required, maxLevel));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error loading achievements: " + e.getMessage());
        }

        return result;
    }

    private AchievementRepository repo() {
        return DatabaseProvider.getAchievementRepository();
    }

    public void initialize() {
        repo().insertDefaultAchievements();
    }

    public void syncUser(String login) {
        repo().syncUserAchievements(login);
    }

    public void addCallback(AchievementCallback cb) {
        repo().addCallback(cb);
    }

    public void complete(String login, String code) {
        repo().completeAchievement(login, code);
    }

    public int getTotalLevels() {
        return repo().getTotalAchievementsLevels();
    }

    public int getUserLevels(String login) {
        return repo().getTotalUserAchievementsLevels(login);
    }

    public int getMaxLevel(String code) {
        return repo().getMaxAchievementLevel(code);
    }

    public Connection getConnection() {
        try {
            return repo().getConnection();
        } catch (Exception e) {
            return null;
        }
    }
}

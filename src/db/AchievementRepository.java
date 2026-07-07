package db;

import util.AchievementCallback;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface AchievementRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addCallback(AchievementCallback cb);
    void addAchievement(String code, String title, String description);
    void addAchievementLevel(String code, int level, int requiredProgress, int xpReward);
    int getMaxAchievementLevel(String code);
    int getTotalAchievementsLevels();
    int getTotalUserAchievementsLevels(String login);
    void insertDefaultAchievements();
    void syncUserAchievements(String login);
    Map<String, Integer> getUserProgress(String login);
    void completeAchievement(String login, String code);
}
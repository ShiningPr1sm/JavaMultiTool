package db.impl;

import db.AchievementRepository;
import util.AchievementCallback;
import util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AchievementRepositoryImpl implements AchievementRepository {
    private static final String DB_PATH = util.AppPaths.DB_ACHIEVEMENTS;
    private final List<AchievementCallback> callbacks = new ArrayList<>();
    private final AchievementDefDao defDao = new AchievementDefDao();
    private final UserProgressDao progressDao = new UserProgressDao();

    @Override
    public void addCallback(AchievementCallback cb) {
        callbacks.add(cb);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    @Override
    public void initializeDatabase() {
        defDao.initializeDatabase();
    }

    @Override
    public void addAchievement(String code, String title, String description) {
        defDao.addAchievement(code, title, description);
    }

    @Override
    public void addAchievementLevel(String code, int level, int requiredProgress, int xpReward) {
        defDao.addAchievementLevel(code, level, requiredProgress, xpReward);
    }

    @Override
    public int getMaxAchievementLevel(String code) {
        return defDao.getMaxAchievementLevel(code);
    }

    @Override
    public int getTotalAchievementsLevels() {
        return defDao.getTotalAchievementsLevels();
    }

    @Override
    public int getTotalUserAchievementsLevels(String login) {
        return progressDao.getTotalUserAchievementsLevels(login);
    }

    @Override
    public void insertDefaultAchievements() {
        defDao.insertDefaultAchievements();
    }

    @Override
    public void syncUserAchievements(String login) {
        progressDao.syncUserAchievements(login);
    }

    @Override
    public Map<String, Integer> getUserProgress(String login) {
        return progressDao.getUserProgress(login);
    }

    @Override
    public void completeAchievement(String login, String code) {
        try (Connection conn = getConnection()) {
            PreparedStatement getState = conn.prepareStatement(
                    "SELECT level, progress FROM user_achievements WHERE user_login=? AND achievement_code=?");
            getState.setString(1, login);
            getState.setString(2, code);
            ResultSet rs = getState.executeQuery();
            if (!rs.next()) return;

            int level = rs.getInt("level");
            int progress = rs.getInt("progress") + 1;

            PreparedStatement getLevel = conn.prepareStatement(
                    "SELECT required_progress, xp_reward FROM achievement_levels WHERE achievement_code=? AND level=?");
            getLevel.setString(1, code);
            getLevel.setInt(2, level);
            ResultSet lvlRs = getLevel.executeQuery();
            if (!lvlRs.next()) return;

            int required = lvlRs.getInt("required_progress");
            int reward = lvlRs.getInt("xp_reward");
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
                for (AchievementCallback cb : callbacks) {
                    cb.onAchievementLevelUp(login, reward);
                    cb.onAchievementNotification(reward);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("AchievementRepositoryImpl SQL error: " + e.getMessage());
        }
    }
}

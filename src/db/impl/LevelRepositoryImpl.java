package db.impl;

import db.LevelRepository;

import util.AppLogger;
import java.sql.*;

public class LevelRepositoryImpl implements LevelRepository {
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_LEVELS);
    }

    @Override
    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_levels (
                    user_login TEXT PRIMARY KEY,
                    xp INTEGER NOT NULL DEFAULT 0
                );
            """);
        } catch (SQLException e) {
            AppLogger.error("LevelRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public void ensureUserEntry(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO user_levels (user_login, xp) VALUES (?, 0)")) {
            stmt.setString(1, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("LevelRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int getXP(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT xp FROM user_levels WHERE user_login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("xp");
            }
        } catch (SQLException e) {
            AppLogger.error("LevelRepositoryImpl SQL error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int getLevel(String login) {
        return calculateLevel(getXP(login));
    }

    @Override
    public void addXP(String login, int amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE user_levels SET xp = xp + ? WHERE user_login = ?")) {
            stmt.setInt(1, amount);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("LevelRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int calculateLevel(int xp) {
        int level = 0;
        int threshold = 100;
        while (xp >= threshold) {
            xp -= threshold;
            threshold += 50;
            level++;
        }
        return level;
    }
}

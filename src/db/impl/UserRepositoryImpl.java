package db.impl;

import db.UserRepository;
import util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserRepositoryImpl implements UserRepository {
    private static final String DB_PATH = util.AppPaths.DB_USER;

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    @Override
    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    nickname TEXT DEFAULT '',
                    reg_time TEXT DEFAULT CURRENT_TIMESTAMP,
                    last_login TEXT DEFAULT CURRENT_TIMESTAMP,
                    save_login INTEGER DEFAULT 0,
                    theme TEXT DEFAULT 'Original Dark',
                    close_to_tray INTEGER DEFAULT 0
                );
            """);
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public boolean register(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            stmt.setString(3, login);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean checkLogin(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE login = ? AND password = ?")) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getNickname(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT nickname FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
        return login;
    }

    @Override
    public void setSaveLogin(String login, boolean enabled) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET save_login = ? WHERE login = ?")) {
            stmt.setInt(1, enabled ? 1 : 0);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public boolean isSaveLoginEnabled(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT save_login FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("save_login") == 1;
            }
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean checkPassword(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String currentPassword = rs.getString("password");
                return currentPassword.equals(password);
            }
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void updatePassword(String login, String newPassword) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password = ? WHERE login = ?")) {
            stmt.setString(1, newPassword);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public String getRegistrationDate(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT reg_time FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("reg_time");
            }
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
        return "Unknown";
    }

    @Override
    public String getLastLoginDate(String login) {
        String sql = "SELECT last_login FROM users WHERE login = ?";
        String lastLogin = "Unknown";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastLogin = rs.getString("last_login");
                }
            }

        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }

        return lastLogin;
    }

    @Override
public void updateLastLoginDate(String login) {
        String sql = "UPDATE users SET last_login = ? WHERE login = ?";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, now);
            stmt.setString(2, login);
            stmt.executeUpdate();

        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public String getAutoLoginUser() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT login FROM users WHERE save_login = 1 LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("login");
            }
        } catch (SQLException e) {
            AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setTheme(String login, String themeName) {
        String sql = "UPDATE users SET theme = ? WHERE login = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, themeName);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException e) { AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage()); }
    }

    @Override
    public String getTheme(String login) {
        String sql = "SELECT theme FROM users WHERE login = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("theme");
        } catch (SQLException e) { AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage()); }
        return "Original Dark";
    }

    @Override
    public void setCloseToTray(String login, boolean enabled) {
        String sql = "UPDATE users SET close_to_tray = ? WHERE login = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException e) { AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage()); }
    }

    @Override
    public boolean isCloseToTrayEnabled(String login) {
        String sql = "SELECT close_to_tray FROM users WHERE login = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 1;
        } catch (SQLException e) { AppLogger.error("UserRepositoryImpl SQL error: " + e.getMessage()); }
        return false;
    }
}

package db.impl;

import db.NotificationRecord;
import db.NotificationRepository;
import util.AppLogger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepositoryImpl implements NotificationRepository {
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_BDAYS);
    }

    @Override
    public void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS notification_log (
              id            INTEGER PRIMARY KEY AUTOINCREMENT,
              type          TEXT    NOT NULL,
              reference_id  INTEGER NOT NULL,
              days_before   INTEGER NOT NULL,
              notified_at   TEXT    NOT NULL,
              dismissed     INTEGER DEFAULT 0
            );
            """;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public boolean hasBeenNotifiedToday(int referenceId, String type, int daysBefore) {
        String today = LocalDate.now().toString();
        String sql = """
            SELECT COUNT(*) FROM notification_log
             WHERE type = ?
               AND reference_id = ?
               AND days_before = ?
               AND notified_at = ?
               AND dismissed = 0
            """;
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, type);
            st.setInt(2, referenceId);
            st.setInt(3, daysBefore);
            st.setString(4, today);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
            return true;
        }
    }

    @Override
    public void insertNotification(String type, int referenceId, int daysBefore) {
        String today = LocalDate.now().toString();
        String sql = "INSERT INTO notification_log(type, reference_id, days_before, notified_at) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, type);
            st.setInt(2, referenceId);
            st.setInt(3, daysBefore);
            st.setString(4, today);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<NotificationRecord> getActiveNotifications() {
        String sql = """
            SELECT id, type, reference_id, days_before, notified_at, dismissed
              FROM notification_log
             WHERE dismissed = 0
             ORDER BY id DESC
            """;
        List<NotificationRecord> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                result.add(new NotificationRecord(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getInt("reference_id"),
                        rs.getInt("days_before"),
                        rs.getString("notified_at"),
                        rs.getBoolean("dismissed")
                ));
            }
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void dismissNotification(int id) {
        String sql = "UPDATE notification_log SET dismissed = 1 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM notification_log WHERE dismissed = 0";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            AppLogger.error("NotificationRepositoryImpl SQL error: " + e.getMessage());
            return 0;
        }
    }
}

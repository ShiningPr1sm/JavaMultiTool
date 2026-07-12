package db.impl;

import util.AppLogger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackedAppDao {
    private static final String DB_PATH = util.AppPaths.DB_WORKFLOW;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public void addTrackedApp(String appName, String exeName) {
        String sql = "INSERT OR IGNORE INTO tracked_apps(app_name, exe_name) VALUES(?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appName);
            pstmt.setString(2, exeName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("TrackedAppDao error: " + e.getMessage());
        }
    }

    public void deleteTrackedApp(int id) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM daily_stats WHERE item_id = ? AND type = 0")) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tracked_apps WHERE id = ?")) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            AppLogger.error("TrackedAppDao error: " + e.getMessage());
        }
    }

    public void updateAppName(int id, String newName) {
        String sql = "UPDATE tracked_apps SET app_name = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("TrackedAppDao error: " + e.getMessage());
        }
    }

    public List<Object[]> getTrackedAppsFull() {
        List<Object[]> apps = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, app_name, exe_name FROM tracked_apps")) {
            while (rs.next()) {
                apps.add(new Object[]{rs.getInt("id"), rs.getString("app_name"), rs.getString("exe_name")});
            }
        } catch (SQLException e) {
            AppLogger.error("TrackedAppDao error: " + e.getMessage());
        }
        return apps;
    }
}

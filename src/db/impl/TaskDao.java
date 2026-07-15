package db.impl;

import util.AppLogger;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDao {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_WORKFLOW);
    }

    public void addTask(String name, String desc) {
        String sql = "INSERT OR IGNORE INTO tasks(task_name, description, created_at) VALUES(?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("TaskDao SQL error: " + e.getMessage());
        }
    }

    public void deleteTask(int id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            try (PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM daily_stats WHERE item_id = ? AND type = 1")) {
                pstmt2.setInt(1, id);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            AppLogger.error("TaskDao SQL error: " + e.getMessage());
        }
    }

    public List<Object[]> getTasksFull() {
        List<Object[]> tasks = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, task_name, description FROM tasks")) {
            while (rs.next()) {
                tasks.add(new Object[]{rs.getInt("id"), rs.getString("task_name"), rs.getString("description")});
            }
        } catch (SQLException e) {
            AppLogger.error("TaskDao SQL error: " + e.getMessage());
        }
        return tasks;
    }
}

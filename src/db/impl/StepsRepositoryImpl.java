package db.impl;

import db.StepsEntry;
import db.StepsRepository;
import util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StepsRepositoryImpl implements StepsRepository {

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_DAILY);
    }

    @Override
    public void initializeDatabase() {
        String stepsSql = """
            CREATE TABLE IF NOT EXISTS steps_entries (
              id          INTEGER PRIMARY KEY AUTOINCREMENT,
              date        TEXT NOT NULL,
              steps       INTEGER NOT NULL,
              calories    INTEGER NOT NULL,
              calc_mode   TEXT NOT NULL,
              weight_used REAL
            );
            """;
        String settingsSql = """
            CREATE TABLE IF NOT EXISTS settings (
              key   TEXT PRIMARY KEY,
              value TEXT
            );
            """;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(stepsSql);
            st.execute(settingsSql);
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public void addSteps(String date, int steps, int calories, String calcMode, Double weightUsed) {
        String sql = "INSERT INTO steps_entries(date, steps, calories, calc_mode, weight_used) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            st.setInt(2, steps);
            st.setInt(3, calories);
            st.setString(4, calcMode);
            if (weightUsed != null) st.setDouble(5, weightUsed);
            else st.setNull(5, Types.REAL);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<StepsEntry> listByDate(String date) throws SQLException {
        String sql = "SELECT id, date, steps, calories, calc_mode, weight_used FROM steps_entries WHERE date = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                List<StepsEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    double w = rs.getDouble("weight_used");
                    boolean hasWeight = !rs.wasNull();
                    entries.add(new StepsEntry(
                            rs.getInt("id"),
                            rs.getString("date"),
                            rs.getInt("steps"),
                            rs.getInt("calories"),
                            rs.getString("calc_mode"),
                            hasWeight ? w : null
                    ));
                }
                return entries;
            }
        }
    }

    @Override
    public void deleteSteps(int id) {
        String sql = "DELETE FROM steps_entries WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int totalSteps(String date) {
        String sql = "SELECT COALESCE(SUM(steps), 0) FROM steps_entries WHERE date = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int totalCalories(String date) {
        String sql = "SELECT COALESCE(SUM(calories), 0) FROM steps_entries WHERE date = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public Double getWeight() {
        String sql = "SELECT value FROM settings WHERE key = 'weight'";
        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
            }
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        try {
                            return Double.parseDouble(rs.getString("value"));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setWeight(double weight) {
        String sql = "INSERT INTO settings(key, value) VALUES('weight', ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, String.valueOf(weight));
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("StepsRepositoryImpl SQL error: " + e.getMessage());
        }
    }
}
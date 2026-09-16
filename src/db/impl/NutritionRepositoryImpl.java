package db.impl;

import db.FoodEntry;
import db.NutritionRepository;
import util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NutritionRepositoryImpl implements NutritionRepository {

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_DAILY);
    }

    @Override
    public void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS food_entries (
              id       INTEGER PRIMARY KEY AUTOINCREMENT,
              date     TEXT NOT NULL,
              meal_name TEXT NOT NULL,
              calories INTEGER NOT NULL
            );
            """;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            AppLogger.error("NutritionRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public void addFood(String date, String mealName, int calories) {
        String sql = "INSERT INTO food_entries(date, meal_name, calories) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            st.setString(2, mealName);
            st.setInt(3, calories);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("NutritionRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<FoodEntry> listByDate(String date) throws SQLException {
        String sql = "SELECT id, date, meal_name, calories FROM food_entries WHERE date = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                List<FoodEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    entries.add(new FoodEntry(
                            rs.getInt("id"),
                            rs.getString("date"),
                            rs.getString("meal_name"),
                            rs.getInt("calories")
                    ));
                }
                return entries;
            }
        }
    }

    @Override
    public void deleteFood(int id) {
        String sql = "DELETE FROM food_entries WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("NutritionRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int totalCalories(String date) {
        String sql = "SELECT COALESCE(SUM(calories), 0) FROM food_entries WHERE date = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("NutritionRepositoryImpl SQL error: " + e.getMessage());
        }
        return 0;
    }
}
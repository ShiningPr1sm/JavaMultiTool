package db.impl;

import db.PurchaseEntry;
import db.PurchasesRepository;
import util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchasesRepositoryImpl implements PurchasesRepository {

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_DAILY);
    }

    @Override
    public void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS purchases (
              id       INTEGER PRIMARY KEY AUTOINCREMENT,
              date     TEXT NOT NULL,
              item_name TEXT NOT NULL,
              price    REAL NOT NULL,
              currency TEXT NOT NULL
            );
            """;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            AppLogger.error("PurchasesRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public void addPurchase(String date, String itemName, double price, String currency) {
        String sql = "INSERT INTO purchases(date, item_name, price, currency) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            st.setString(2, itemName);
            st.setDouble(3, price);
            st.setString(4, currency);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("PurchasesRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<PurchaseEntry> listByDate(String date) throws SQLException {
        String sql = "SELECT id, date, item_name, price, currency FROM purchases WHERE date = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                List<PurchaseEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    entries.add(new PurchaseEntry(
                            rs.getInt("id"),
                            rs.getString("date"),
                            rs.getString("item_name"),
                            rs.getDouble("price"),
                            rs.getString("currency")
                    ));
                }
                return entries;
            }
        }
    }

    @Override
    public void deletePurchase(int id) {
        String sql = "DELETE FROM purchases WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("PurchasesRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<String> getCurrenciesForDate(String date) throws SQLException {
        String sql = "SELECT DISTINCT currency FROM purchases WHERE date = ? ORDER BY currency ASC";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            try (ResultSet rs = st.executeQuery()) {
                List<String> currencies = new ArrayList<>();
                while (rs.next()) currencies.add(rs.getString(1));
                return currencies;
            }
        }
    }

    @Override
    public double totalForCurrency(String date, String currency) {
        String sql = "SELECT COALESCE(SUM(price), 0) FROM purchases WHERE date = ? AND currency = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, date);
            st.setString(2, currency);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            AppLogger.error("PurchasesRepositoryImpl SQL error: " + e.getMessage());
        }
        return 0;
    }
}
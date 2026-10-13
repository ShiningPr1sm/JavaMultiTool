package db.impl;

import db.ProductEntry;
import db.ProductRepository;
import util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepositoryImpl implements ProductRepository {

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_DAILY);
    }

    @Override
    public void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS products (
              id       INTEGER PRIMARY KEY AUTOINCREMENT,
              name     TEXT NOT NULL UNIQUE,
              price    REAL,
              calories INTEGER,
              currency TEXT
            );
            """;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            AppLogger.error("ProductRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public int addProduct(String name, Double price, Integer calories, String currency) {
        String sql = "INSERT INTO products(id, name, price, calories, currency) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection()) {
            int nextId = nextFreeId(conn);
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, nextId);
                st.setString(2, name);
                setNullableDouble(st, 3, price);
                setNullableInt(st, 4, calories);
                st.setString(5, currency);
                st.executeUpdate();
            }
            return nextId;
        } catch (SQLException e) {
            AppLogger.error("ProductRepositoryImpl SQL error: " + e.getMessage());
        }
        return -1;
    }

    private int nextFreeId(Connection conn) throws SQLException {
        String sql = """
            SELECT CASE
              WHEN NOT EXISTS (SELECT 1 FROM products WHERE id = 1) THEN 1
              ELSE (SELECT MIN(t.id + 1) FROM products t WHERE t.id + 1 NOT IN (SELECT id FROM products))
            END
            """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 1;
    }

    @Override
    public void updateProduct(int id, String name, Double price, Integer calories, String currency) {
        String sql = "UPDATE products SET name = ?, price = ?, calories = ?, currency = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, name);
            setNullableDouble(st, 2, price);
            setNullableInt(st, 3, calories);
            st.setString(4, currency);
            st.setInt(5, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("ProductRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public void deleteProduct(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("ProductRepositoryImpl SQL error: " + e.getMessage());
        }
    }

    @Override
    public List<ProductEntry> listAll() {
        String sql = "SELECT id, name, price, calories, currency FROM products ORDER BY id ASC";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<ProductEntry> entries = new ArrayList<>();
            while (rs.next()) {
                double price = rs.getDouble("price");
                boolean priceNull = rs.wasNull();
                int calories = rs.getInt("calories");
                boolean caloriesNull = rs.wasNull();
                entries.add(new ProductEntry(
                        rs.getInt("id"),
                        rs.getString("name"),
                        priceNull ? null : price,
                        caloriesNull ? null : calories,
                        rs.getString("currency")
                ));
            }
            return entries;
        } catch (SQLException e) {
            AppLogger.error("ProductRepositoryImpl SQL error: " + e.getMessage());
            return List.of();
        }
    }

    private static void setNullableDouble(PreparedStatement st, int idx, Double v) throws SQLException {
        if (v == null) st.setNull(idx, Types.REAL);
        else st.setDouble(idx, v);
    }

    private static void setNullableInt(PreparedStatement st, int idx, Integer v) throws SQLException {
        if (v == null) st.setNull(idx, Types.INTEGER);
        else st.setInt(idx, v);
    }
}
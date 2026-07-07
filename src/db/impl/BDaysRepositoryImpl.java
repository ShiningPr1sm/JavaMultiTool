package db.impl;

import db.BDaysRepository;
import db.BirthdayRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BDaysRepositoryImpl implements BDaysRepository {
    private static final String DB_PATH = util.AppPaths.DB_BDAYS;

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    @Override
    public void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS birthdays (
              id         INTEGER PRIMARY KEY,
              name       TEXT    NOT NULL,
              bday_date  TEXT    NOT NULL
            );
            """;
        try (Connection conn = getConnection();
             Statement  st  = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<BirthdayRecord> getAllBirthdays() throws SQLException {
        String sql = "SELECT id, name, bday_date FROM birthdays ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<BirthdayRecord> records = new ArrayList<>();
            while (rs.next()) {
                records.add(new BirthdayRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("bday_date")
                ));
            }
            return records;
        }
    }

    @Override
    public void addBirthday(String name, String dateStr) {
        String sql = "INSERT INTO birthdays(name,bday_date) VALUES(?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, name);
            st.setString(2, dateStr);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBirthday(int id) {
        String sql = "DELETE FROM birthdays WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateBirthday(int id, String name, String dateStr) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE birthdays SET name = ?, bday_date = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, dateStr);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }
}

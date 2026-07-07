package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BDaysRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    List<BirthdayRecord> getAllBirthdays() throws SQLException;
    void addBirthday(String name, String dateStr);
    void removeBirthday(int id);
    void updateBirthday(int id, String name, String dateStr) throws SQLException;
}
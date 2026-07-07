package db;

import java.sql.Connection;
import java.sql.SQLException;

public interface LevelRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void ensureUserEntry(String login);
    int getXP(String login);
    int getLevel(String login);
    void addXP(String login, int amount);
    int calculateLevel(int xp);
}
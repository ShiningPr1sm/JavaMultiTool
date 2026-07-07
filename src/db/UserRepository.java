package db;

import java.sql.Connection;
import java.sql.SQLException;

public interface UserRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    boolean register(String login, String password);
    boolean checkLogin(String login, String password);
    String getNickname(String login);
    void setSaveLogin(String login, boolean enabled);
    boolean isSaveLoginEnabled(String login);
    boolean checkPassword(String login, String password);
    void updatePassword(String login, String newPassword);
    String getRegistrationDate(String login);
    String getLastLoginDate(String login);
    void updateLastLoginDate(String login);
    String getAutoLoginUser();
    void setTheme(String login, String themeName);
    String getTheme(String login);
    void setCloseToTray(String login, boolean enabled);
    boolean isCloseToTrayEnabled(String login);
}
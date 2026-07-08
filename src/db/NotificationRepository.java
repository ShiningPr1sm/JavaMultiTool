package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NotificationRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    boolean hasBeenNotifiedToday(int referenceId, String type, int daysBefore);
    void insertNotification(String type, int referenceId, int daysBefore);
    List<NotificationRecord> getActiveNotifications();
    void dismissNotification(int id);
    int countActive();
}

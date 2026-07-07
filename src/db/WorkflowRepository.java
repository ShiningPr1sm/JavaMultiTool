package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface WorkflowRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addTime(int itemId, int type, int seconds);
    void deleteTrackedAppFromDB(int id);
    void addTrackedApp(String appName, String exeName);
    void addTask(String name, String desc);
    List<Object[]> getTrackedAppsFull();
    int getSecondsToday(int itemId, int type);
    void updateAppName(int id, String newName);
    Map<String, Integer> getDaySummary(String date);
    int[] getHourlyStats(String date, String appName);
    List<String> getAvailableDates();
    StatResult getPeriodStats(String endDateStr, String appName, int days);
    StatResult getCalendarMonthStats(String dateStr, String appName);
    List<Object[]> getTasksFull();
    void deleteTask(int id);
}
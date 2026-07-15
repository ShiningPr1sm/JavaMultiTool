package db.impl;

import db.StatResult;
import db.WorkflowRepository;
import util.AppLogger;

import java.sql.*;
import java.util.List;
import java.util.Map;

public class WorkflowRepositoryImpl implements WorkflowRepository {
    private final TrackedAppDao trackedAppDao = new TrackedAppDao();
    private final TaskDao taskDao = new TaskDao();
    private final DailyStatsDao dailyStatsDao = new DailyStatsDao();

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_WORKFLOW);
    }

    @Override
    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tracked_apps (id INTEGER PRIMARY KEY AUTOINCREMENT, app_name TEXT, exe_name TEXT UNIQUE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_name TEXT UNIQUE, " +
                    "description TEXT, " +
                    "created_at TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_stats (" +
                    "date TEXT, hour INTEGER, item_id INTEGER, type INTEGER, seconds_spent INTEGER, " +
                    "PRIMARY KEY (date, hour, item_id, type))");
        } catch (SQLException e) {
            AppLogger.error("DB Init error: " + e.getMessage());
        }
    }

    @Override
    public void addTime(int itemId, int type, int seconds) {
        dailyStatsDao.addTime(itemId, type, seconds);
    }

    @Override
    public void deleteTrackedAppFromDB(int id) {
        trackedAppDao.deleteTrackedApp(id);
    }

    @Override
    public void addTrackedApp(String appName, String exeName) {
        trackedAppDao.addTrackedApp(appName, exeName);
    }

    @Override
    public void addTask(String name, String desc) {
        taskDao.addTask(name, desc);
    }

    @Override
    public List<Object[]> getTrackedAppsFull() {
        return trackedAppDao.getTrackedAppsFull();
    }

    @Override
    public int getSecondsToday(int itemId, int type) {
        return dailyStatsDao.getSecondsToday(itemId, type);
    }

    @Override
    public void updateAppName(int id, String newName) {
        trackedAppDao.updateAppName(id, newName);
    }

    @Override
    public Map<String, Integer> getDaySummary(String date) {
        return dailyStatsDao.getDaySummary(date);
    }

    @Override
    public int[] getHourlyStats(String date, String appName) {
        return dailyStatsDao.getHourlyStats(date, appName);
    }

    @Override
    public List<String> getAvailableDates() {
        return dailyStatsDao.getAvailableDates();
    }

    @Override
    public StatResult getPeriodStats(String endDateStr, String appName, int days) {
        return dailyStatsDao.getPeriodStats(endDateStr, appName, days);
    }

    @Override
    public StatResult getCalendarMonthStats(String dateStr, String appName) {
        return dailyStatsDao.getCalendarMonthStats(dateStr, appName);
    }

    @Override
    public List<Object[]> getTasksFull() {
        return taskDao.getTasksFull();
    }

    @Override
    public void deleteTask(int id) {
        taskDao.deleteTask(id);
    }
}

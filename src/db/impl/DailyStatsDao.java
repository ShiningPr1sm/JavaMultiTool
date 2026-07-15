package db.impl;

import db.StatResult;
import util.AppLogger;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DailyStatsDao {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + util.AppPaths.DB_WORKFLOW);
    }

    public void addTime(int itemId, int type, int seconds) {
        String today = LocalDate.now().toString();
        int hour = java.time.LocalTime.now().getHour();

        String sql = "INSERT INTO daily_stats(date, hour, item_id, type, seconds_spent) VALUES(?,?,?,?,?) " +
                "ON CONFLICT(date, hour, item_id, type) DO UPDATE SET seconds_spent = seconds_spent + excluded.seconds_spent";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            pstmt.setInt(2, hour);
            pstmt.setInt(3, itemId);
            pstmt.setInt(4, type);
            pstmt.setInt(5, seconds);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("DailyStatsDao SQL error: " + e.getMessage());
        }
    }

    public int getSecondsToday(int itemId, int type) {
        String sql = "SELECT SUM(seconds_spent) FROM daily_stats WHERE date = ? AND item_id = ? AND type = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setInt(2, itemId);
            pstmt.setInt(3, type);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("DB Error in getSecondsToday: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getDaySummary(String date) {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = """
        SELECT COALESCE(ta.app_name, t.task_name, 'Unknown') as name, SUM(ds.seconds_spent) as total
        FROM daily_stats ds
        LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0
        LEFT JOIN tasks t ON ds.item_id = t.id AND ds.type = 1
        WHERE ds.date = ?
        GROUP BY name ORDER BY total DESC
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) data.put(rs.getString("name"), rs.getInt("total"));
        } catch (SQLException e) {
            AppLogger.error("DailyStatsDao SQL error: " + e.getMessage());
        }
        return data;
    }

    public int[] getHourlyStats(String date, String appName) {
        int[] hours = new int[24];
        String sql = "SELECT hour, SUM(seconds_spent) FROM daily_stats ds " +
                "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                "WHERE ds.date = ? " +
                (appName.equals("ALL") ? "" : "AND ta.app_name = ? ") +
                "GROUP BY hour";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            if (!appName.equals("ALL")) pstmt.setString(2, appName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) hours[rs.getInt(1)] = rs.getInt(2);
        } catch (SQLException e) {
            AppLogger.error("DailyStatsDao SQL error: " + e.getMessage());
        }
        return hours;
    }

    public List<String> getAvailableDates() {
        List<String> dates = new ArrayList<>();
        String sql = "SELECT DISTINCT date FROM daily_stats ORDER BY date DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dates.add(rs.getString("date"));
            }
        } catch (SQLException e) { AppLogger.error("DailyStatsDao SQL error: " + e.getMessage()); }

        if (dates.isEmpty()) {
            dates.add(LocalDate.now().toString());
        }
        return dates;
    }

    public StatResult getPeriodStats(String endDateStr, String appName, int days) {
        StatResult result = new StatResult(days);
        LocalDate endDate = LocalDate.parse(endDateStr);

        for (int i = 0; i < days; i++) {
            LocalDate target = endDate.minusDays(days - 1 - i);
            result.labels[i] = target.toString();

            String sql = "SELECT SUM(seconds_spent) FROM daily_stats ds " +
                    "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                    "WHERE ds.date = ? " +
                    (appName.equals("ALL") ? "" : "AND ta.app_name = ? ");

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, target.toString());
                if (!appName.equals("ALL"))
                    pstmt.setString(2, appName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    result.values[i] = rs.getInt(1);
            } catch (SQLException e) {
                AppLogger.error("DailyStatsDao SQL error: " + e.getMessage());
            }
        }
        return result;
    }

    public StatResult getCalendarMonthStats(String dateStr, String appName) {
        LocalDate selectedDate = LocalDate.parse(dateStr);
        int daysInMonth = selectedDate.lengthOfMonth();
        StatResult result = new StatResult(daysInMonth);
        LocalDate firstDay = selectedDate.withDayOfMonth(1);

        for (int i = 0; i < daysInMonth; i++) {
            LocalDate target = firstDay.plusDays(i);
            result.labels[i] = String.valueOf(i + 1);

            String sql = "SELECT SUM(seconds_spent) FROM daily_stats ds " +
                    "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                    "WHERE ds.date = ? " +
                    (appName.equals("ALL") ? "" : "AND ta.app_name = ? ");
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, target.toString());
                if (!appName.equals("ALL"))
                    pstmt.setString(2, appName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    result.values[i] = rs.getInt(1);
            } catch (SQLException e) {
                AppLogger.error("DailyStatsDao SQL error: " + e.getMessage());
            }
        }
        return result;
    }
}

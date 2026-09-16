package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface StepsRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addSteps(String date, int steps, int calories, String calcMode, Double weightUsed);
    List<StepsEntry> listByDate(String date) throws SQLException;
    void deleteSteps(int id);
    int totalSteps(String date);
    int totalCalories(String date);
    Double getWeight();
    void setWeight(double weight);
}
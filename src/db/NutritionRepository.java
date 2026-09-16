package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NutritionRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addFood(String date, String mealName, int calories);
    List<FoodEntry> listByDate(String date) throws SQLException;
    void deleteFood(int id);
    int totalCalories(String date);
}
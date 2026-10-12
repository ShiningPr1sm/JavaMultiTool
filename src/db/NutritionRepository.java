package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NutritionRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addFood(String date, String mealName, int calories);
    void addFoodLinked(String date, String mealName, int calories, int purchaseId);
    List<FoodEntry> listByDate(String date) throws SQLException;
    void deleteFood(int id);
    void deleteByPurchaseId(int purchaseId);
    int totalCalories(String date);
}
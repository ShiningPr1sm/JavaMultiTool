package db.impl;

import db.PurchaseEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nutrition + Purchases repository logic on a temporary SQLite database
 * (util.AppPaths.DB_DAILY is redirected to a temp file so the real user data
 * under %APPDATA% is never touched). Covers the purchases-calories link:
 * addFoodLinked rows must be removable via deleteByPurchaseId.
 */
class DailyRepositoryTest {

    private Path tempDb;
    private String originalDbPath;

    private NutritionRepositoryImpl nutrition;
    private PurchasesRepositoryImpl purchases;

    @BeforeEach
    void setup() throws Exception {
        originalDbPath = util.AppPaths.DB_DAILY;
        tempDb = Files.createTempFile("jmt_test_daily_", ".db");
        util.AppPaths.DB_DAILY = tempDb.toString();

        nutrition = new NutritionRepositoryImpl();
        purchases = new PurchasesRepositoryImpl();
        nutrition.initializeDatabase();
        purchases.initializeDatabase();
    }

    @AfterEach
    void teardown() {
        util.AppPaths.DB_DAILY = originalDbPath;
    }

    @Test
    void purchasesAddListTotalAndDelete() throws SQLException {
        String date = "2026-11-06";
        int id1 = purchases.addPurchase(date, "Bread", 2.50, "EUR");
        int id2 = purchases.addPurchase(date, "Milk", 1.20, "EUR");
        assertTrue(id1 > 0 && id2 > 0 && id1 != id2, "purchase ids must be distinct and positive");

        List<PurchaseEntry> entries = purchases.listByDate(date);
        assertEquals(2, entries.size());
        assertEquals("Bread", entries.get(0).itemName());

        assertEquals(List.of("EUR"), purchases.getCurrenciesForDate(date));
        assertEquals(3.70, purchases.totalForCurrency(date, "EUR"), 1e-9); // 2.50 + 1.20

        purchases.deletePurchase(id1);
        assertEquals(1, purchases.listByDate(date).size());
    }

    @Test
    void nutritionTotalsListAndLinkedCascade() throws SQLException {
        String date = "2026-11-06";
        nutrition.addFood(date, "Breakfast", 300);
        int purchaseId = purchases.addPurchase(date, "Chocolate", 3.00, "EUR");
        nutrition.addFoodLinked(date, "Chocolate", 250, purchaseId);
        nutrition.addFoodLinked(date, "Chocolate (2nd)", 250, purchaseId);

        assertEquals(800, nutrition.totalCalories(date));
        assertEquals(3, nutrition.listByDate(date).size());

        // deleting the purchase removes exactly its linked calories rows
        purchases.deletePurchase(purchaseId);
        nutrition.deleteByPurchaseId(purchaseId);
        List<db.FoodEntry> rest = nutrition.listByDate(date);
        assertEquals(1, rest.size());
        assertEquals("Breakfast", rest.get(0).mealName());
        assertEquals(300, nutrition.totalCalories(date));
    }

    @Test
    void datesAreIsolated() throws SQLException {
        nutrition.addFood("2026-11-06", "A", 100);
        nutrition.addFood("2026-11-07", "B", 200);
        assertEquals(100, nutrition.totalCalories("2026-11-06"));
        assertEquals(200, nutrition.totalCalories("2026-11-07"));
        assertEquals(0, nutrition.totalCalories("2025-01-01"));
    }

    @Test
    void deleteFoodRemovesSingleRow() throws SQLException {
        String date = "2026-11-06";
        nutrition.addFood(date, "A", 100);
        nutrition.addFood(date, "B", 100);
        int firstId = nutrition.listByDate(date).stream()
                .min(Comparator.comparingInt(db.FoodEntry::id)).orElseThrow().id();
        nutrition.deleteFood(firstId);
        List<db.FoodEntry> rest = nutrition.listByDate(date);
        assertEquals(1, rest.size());
        assertEquals("B", rest.get(0).mealName());
    }
}

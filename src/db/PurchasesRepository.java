package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PurchasesRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    void addPurchase(String date, String itemName, double price, String currency);
    List<PurchaseEntry> listByDate(String date) throws SQLException;
    void deletePurchase(int id);
    List<String> getCurrenciesForDate(String date) throws SQLException;
    double totalForCurrency(String date, String currency);
}
package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ProductRepository {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
    int addProduct(String name, Double price, Integer calories, String currency);
    void updateProduct(int id, String name, Double price, Integer calories, String currency);
    void deleteProduct(int id);
    List<ProductEntry> listAll();
}
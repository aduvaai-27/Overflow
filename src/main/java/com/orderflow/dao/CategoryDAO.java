package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.CatalogRow;
import com.orderflow.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private static final String SELECT_BASE =
            "SELECT c.*, " +
            "(SELECT GROUP_CONCAT(s.name, ', ') FROM category_suppliers cs " +
            " JOIN suppliers s ON cs.supplier_id = s.id WHERE cs.category_id = c.id) AS suppliers_display " +
            "FROM categories c ";

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY c.name";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<CatalogRow> findCatalog(int categoryId) {
        List<CatalogRow> rows = new ArrayList<>();
        String sql = "SELECT COALESCE(s.id, 0) AS supplier_id, COALESCE(s.name, '-') AS supplier_name, " +
                     "p.id AS product_id, p.name AS product_name, p.sku AS sku, p.selling_price AS price " +
                     "FROM products p LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                     "WHERE p.category_id = ? AND p.active = 1 ORDER BY supplier_name, p.name";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CatalogRow(rs.getInt("supplier_id"), rs.getString("supplier_name"),
                            rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getString("sku"), rs.getDouble("price")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public boolean add(String name) {
        String sql = "INSERT INTO categories(name) VALUES (?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(int id, String name) {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try (PreparedStatement ps1 = DatabaseConnection.getConnection().prepareStatement(
                "DELETE FROM category_suppliers WHERE category_id = ?")) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        String sql = "DELETE FROM categories WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"), rs.getString("suppliers_display"));
    }
}

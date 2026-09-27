package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String SELECT_BASE =
            "SELECT p.*, c.name AS category_name, " +
            "(SELECT GROUP_CONCAT(s.name, ', ') FROM category_suppliers cs " +
            " JOIN suppliers s ON cs.supplier_id = s.id WHERE cs.category_id = c.id) AS supplier_names, " +
            "(SELECT GROUP_CONCAT(cs.supplier_id) FROM category_suppliers cs WHERE cs.category_id = c.id) AS supplier_ids " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id ";

    public List<Product> findAllActive() {
        return query(SELECT_BASE + "WHERE p.active = 1 ORDER BY p.name");
    }

    public List<Product> findAll() {
        return query(SELECT_BASE + "ORDER BY p.name");
    }

    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.active = 1 AND (p.name LIKE ? OR p.sku LIKE ?) ORDER BY p.name";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countLowStock() {
        String sql = "SELECT COUNT(*) AS cnt FROM products WHERE active = 1 AND stock_qty <= min_stock";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean add(Product p) {
        String sql = "INSERT INTO products(name, sku, category_id, purchase_price, selling_price, stock_qty, min_stock, active) " +
                     "VALUES (?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getCategoryId());
            ps.setDouble(4, p.getPurchasePrice());
            ps.setDouble(5, p.getSellingPrice());
            ps.setInt(6, p.getStockQty());
            ps.setInt(7, p.getMinStock());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET name=?, sku=?, category_id=?, purchase_price=?, selling_price=?, " +
                     "stock_qty=?, min_stock=?, active=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getCategoryId());
            ps.setDouble(4, p.getPurchasePrice());
            ps.setDouble(5, p.getSellingPrice());
            ps.setInt(6, p.getStockQty());
            ps.setInt(7, p.getMinStock());
            ps.setInt(8, p.isActive() ? 1 : 0);
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deactivate(int id) {
        String sql = "UPDATE products SET active = 0 WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean adjustStock(int productId, int deltaQty, String reason) {
        Connection conn = DatabaseConnection.getConnection();
        String updateSql = "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?";
        String logSql = "INSERT INTO inventory_transactions(product_id, change_qty, reason, transaction_date) VALUES (?,?,?,?)";
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateSql)) {
                ps1.setInt(1, deltaQty);
                ps1.setInt(2, productId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(logSql)) {
                ps2.setInt(1, productId);
                ps2.setInt(2, deltaQty);
                ps2.setString(3, reason);
                ps2.setString(4, java.time.LocalDateTime.now().toString());
                ps2.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) AS cnt FROM products WHERE active = 1";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private List<Product> query(String sql) {
        List<Product> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("sku"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getDouble("purchase_price"),
                rs.getDouble("selling_price"),
                rs.getInt("stock_qty"),
                rs.getInt("min_stock"),
                rs.getInt("active") == 1
        );
        String supplierIdsRaw = rs.getString("supplier_ids");
        java.util.Set<Integer> supplierIds = new java.util.HashSet<>();
        if (supplierIdsRaw != null && !supplierIdsRaw.isBlank()) {
            for (String idStr : supplierIdsRaw.split(",")) {
                supplierIds.add(Integer.parseInt(idStr.trim()));
            }
        }
        p.setSupplierIds(supplierIds);
        p.setSupplierNamesDisplay(rs.getString("supplier_names"));
        return p;
    }
}

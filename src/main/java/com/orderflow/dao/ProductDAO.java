package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String SELECT_BASE =
            "SELECT p.*, c.name AS category_name, s.name AS supplier_name " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "LEFT JOIN suppliers s ON p.supplier_id = s.id ";

    public List<Product> findAllActive() {
        return query(SELECT_BASE + "WHERE p.active = 1 ORDER BY p.name");
    }

    public List<Product> findAll() {
        return query(SELECT_BASE + "ORDER BY p.name");
    }

    /** Only the products owned by this exact supplier - what a "select supplier, then product" flow should offer. */
    public List<Product> findAllActiveBySupplier(int supplierId) {
        List<Product> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.active = 1 AND p.supplier_id = ? ORDER BY p.name";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
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

    /** Used by the background StockAlertMonitor thread (multithreading topic). */
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
        String sql = "INSERT INTO products(name, sku, category_id, supplier_id, purchase_price, selling_price, stock_qty, min_stock, active) " +
                     "VALUES (?,?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getCategoryId());
            setNullableInt(ps, 4, p.getSupplierId());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSellingPrice());
            ps.setInt(7, p.getStockQty());
            ps.setInt(8, p.getMinStock());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET name=?, sku=?, category_id=?, supplier_id=?, purchase_price=?, selling_price=?, " +
                     "stock_qty=?, min_stock=?, active=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getCategoryId());
            setNullableInt(ps, 4, p.getSupplierId());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSellingPrice());
            ps.setInt(7, p.getStockQty());
            ps.setInt(8, p.getMinStock());
            ps.setInt(9, p.isActive() ? 1 : 0);
            ps.setInt(10, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /** Business rule from the spec: deactivate rather than hard-delete products with order history. */
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

    /**
     * Adjusts stock by deltaQty (positive = add stock, negative = remove stock)
     * and writes an inventory_transactions row so the change is traceable.
     */
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
        int supplierId = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : supplierId);
        p.setSupplierName(rs.getString("supplier_name"));
        return p;
    }
}

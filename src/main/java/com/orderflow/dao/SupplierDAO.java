package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupplierDAO {

    private static final String SELECT_BASE =
            "SELECT s.*, " +
            "(SELECT GROUP_CONCAT(c.name, ', ') FROM category_suppliers cs " +
            " JOIN categories c ON cs.category_id = c.id WHERE cs.supplier_id = s.id) AS categories_display " +
            "FROM suppliers s ";

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY s.name";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Which categories (by id) this supplier is currently linked to. */
    public Set<Integer> findCategoryIdsForSupplier(int supplierId) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT category_id FROM category_suppliers WHERE supplier_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("category_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    /** Adds a new supplier and links it to the given categories, in one transaction. */
    public boolean add(Supplier s, Set<Integer> categoryIds) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            int newId;
            String sql = "INSERT INTO suppliers(name, phone, email, address) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getPhone());
                ps.setString(3, s.getEmail());
                ps.setString(4, s.getAddress());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    newId = keys.getInt(1);
                }
            }
            linkCategories(conn, newId, categoryIds);
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

    /** Updates a supplier's details and replaces its category links, in one transaction. */
    public boolean update(Supplier s, Set<Integer> categoryIds) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String sql = "UPDATE suppliers SET name=?, phone=?, email=?, address=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getPhone());
                ps.setString(3, s.getEmail());
                ps.setString(4, s.getAddress());
                ps.setInt(5, s.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category_suppliers WHERE supplier_id = ?")) {
                ps.setInt(1, s.getId());
                ps.executeUpdate();
            }
            linkCategories(conn, s.getId(), categoryIds);
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

    private void linkCategories(Connection conn, int supplierId, Set<Integer> categoryIds) throws SQLException {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        String sql = "INSERT OR IGNORE INTO category_suppliers(category_id, supplier_id) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int categoryId : categoryIds) {
                ps.setInt(1, categoryId);
                ps.setInt(2, supplierId);
                ps.executeUpdate();
            }
        }
    }

    public boolean delete(int id) {
        try (PreparedStatement ps1 = DatabaseConnection.getConnection().prepareStatement(
                "DELETE FROM category_suppliers WHERE supplier_id = ?")) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"));
        s.setCategoriesDisplay(rs.getString("categories_display"));
        return s;
    }
}

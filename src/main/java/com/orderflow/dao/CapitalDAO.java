package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.CapitalTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CapitalDAO {

    public double getCurrentCapital() {
        String sql = "SELECT COALESCE(SUM(amount),0) AS total FROM capital_transactions";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<CapitalTransaction> findAll() {
        List<CapitalTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM capital_transactions ORDER BY id DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new CapitalTransaction(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getString("source"),
                        rs.getString("reason"),
                        rs.getString("transaction_date")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addEntry(double amount, String source, String reason) {
        return addEntry(DatabaseConnection.getConnection(), amount, source, reason);
    }

    public boolean addEntry(Connection conn, double amount, String source, String reason) {
        String sql = "INSERT INTO capital_transactions(amount, source, reason, transaction_date) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, source);
            ps.setString(3, reason);
            ps.setString(4, LocalDateTime.now().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

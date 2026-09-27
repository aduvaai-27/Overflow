package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.SupplierRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages restock requests sent to suppliers. A request moves through
 * phases: Requested -> Shipped -> Completed. Only when it reaches
 * Completed does the app automatically:
 *   1) increase the product's stock by the requested quantity, and
 *   2) pay the supplier out of capital (quantity x unit cost), i.e.
 *      decrease capital and record the purchase in the ledger.
 */
public class SupplierRequestDAO {

    private static final List<String> PHASE_FLOW = List.of("Requested", "Shipped", "Completed");

    private final CapitalDAO capitalDAO = new CapitalDAO();

    public List<String> phaseFlow() {
        return PHASE_FLOW;
    }

    public List<SupplierRequest> findAll() {
        List<SupplierRequest> list = new ArrayList<>();
        String sql = "SELECT r.*, s.name AS supplier_name, p.name AS product_name " +
                     "FROM supplier_requests r " +
                     "JOIN suppliers s ON r.supplier_id = s.id " +
                     "JOIN products p ON r.product_id = p.id " +
                     "ORDER BY r.id DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Creates a new restock request in the "Requested" phase. */
    public boolean createRequest(int supplierId, int productId, int quantity, double unitCost) {
        String sql = "INSERT INTO supplier_requests(supplier_id, product_id, quantity, unit_cost, phase, request_date) " +
                     "VALUES (?,?,?,?,'Requested',?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.setDouble(4, unitCost);
            ps.setString(5, LocalDateTime.now().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Advances a request to its next phase. When the request reaches
     * "Completed", stock is increased and the supplier bill is paid out
     * of capital, all inside one transaction.
     */
    public boolean advancePhase(SupplierRequest request) {
        int currentIndex = PHASE_FLOW.indexOf(request.getPhase());
        if (currentIndex == -1 || currentIndex == PHASE_FLOW.size() - 1) {
            return false; // already completed, or unknown phase
        }
        String nextPhase = PHASE_FLOW.get(currentIndex + 1);

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String updatePhaseSql = nextPhase.equals("Completed")
                    ? "UPDATE supplier_requests SET phase = ?, completed_date = ? WHERE id = ?"
                    : "UPDATE supplier_requests SET phase = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updatePhaseSql)) {
                ps.setString(1, nextPhase);
                if (nextPhase.equals("Completed")) {
                    ps.setString(2, LocalDateTime.now().toString());
                    ps.setInt(3, request.getId());
                } else {
                    ps.setInt(2, request.getId());
                }
                ps.executeUpdate();
            }

            if (nextPhase.equals("Completed")) {
                // 1) Increase stock for the received product
                try (PreparedStatement psStock = conn.prepareStatement(
                        "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?")) {
                    psStock.setInt(1, request.getQuantity());
                    psStock.setInt(2, request.getProductId());
                    psStock.executeUpdate();
                }
                try (PreparedStatement psTxn = conn.prepareStatement(
                        "INSERT INTO inventory_transactions(product_id, change_qty, reason, transaction_date) VALUES (?,?,?,?)")) {
                    psTxn.setInt(1, request.getProductId());
                    psTxn.setInt(2, request.getQuantity());
                    psTxn.setString(3, "Supplier delivery - Request #" + request.getId());
                    psTxn.setString(4, LocalDateTime.now().toString());
                    psTxn.executeUpdate();
                }

                // 2) Pay the supplier out of capital (COD-style, paid on completion)
                double cost = request.getQuantity() * request.getUnitCost();
                String reason = "Purchased " + request.getQuantity() + " x " + request.getProductName()
                        + " from " + request.getSupplierName() + " (Request #" + request.getId() + ")";
                capitalDAO.addEntry(conn, -cost, "Supplier Payment", reason);
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

    private SupplierRequest map(ResultSet rs) throws SQLException {
        SupplierRequest r = new SupplierRequest();
        r.setId(rs.getInt("id"));
        r.setSupplierId(rs.getInt("supplier_id"));
        r.setSupplierName(rs.getString("supplier_name"));
        r.setProductId(rs.getInt("product_id"));
        r.setProductName(rs.getString("product_name"));
        r.setQuantity(rs.getInt("quantity"));
        r.setUnitCost(rs.getDouble("unit_cost"));
        r.setPhase(rs.getString("phase"));
        r.setRequestDate(rs.getString("request_date"));
        r.setCompletedDate(rs.getString("completed_date"));
        return r;
    }
}

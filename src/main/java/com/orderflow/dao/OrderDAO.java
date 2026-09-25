package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private final ProductDAO productDAO = new ProductDAO();

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.name AS customer_name FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.id ORDER BY o.id DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Order> findBetweenDates(String fromDate, String toDate) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.name AS customer_name FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.id " +
                     "WHERE date(o.order_date) BETWEEN date(?) AND date(?) ORDER BY o.id DESC";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<OrderItem> findItemsByOrderId(int orderId) {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT oi.*, p.name AS product_name FROM order_items oi " +
                     "JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setLineTotal(rs.getDouble("line_total"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int createOrder(Order order, List<OrderItem> items) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String insertOrder = "INSERT INTO orders(customer_id, order_date, subtotal, discount, tax, " +
                    "delivery_charge, total, payment_method, payment_status, order_status) VALUES (?,?,?,?,?,?,?,?,?,?)";

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getCustomerId());
                ps.setString(2, LocalDateTime.now().toString());
                ps.setDouble(3, order.getSubtotal());
                ps.setDouble(4, order.getDiscount());
                ps.setDouble(5, order.getTax());
                ps.setDouble(6, order.getDeliveryCharge());
                ps.setDouble(7, order.getTotal());
                ps.setString(8, order.getPaymentMethod());
                ps.setString(9, order.getPaymentStatus());
                ps.setString(10, order.getOrderStatus());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }

            String insertItem = "INSERT INTO order_items(order_id, product_id, quantity, unit_price, line_total) VALUES (?,?,?,?,?)";
            String reduceStock = "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ? AND stock_qty >= ?";
            String logTxn = "INSERT INTO inventory_transactions(product_id, change_qty, reason, transaction_date) VALUES (?,?,?,?)";

            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(reduceStock);
                 PreparedStatement psTxn = conn.prepareStatement(logTxn)) {

                for (OrderItem item : items) {
                    psItem.setInt(1, orderId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getUnitPrice());
                    psItem.setDouble(5, item.getLineTotal());
                    psItem.executeUpdate();

                    psStock.setInt(1, item.getQuantity());
                    psStock.setInt(2, item.getProductId());
                    psStock.setInt(3, item.getQuantity());
                    int rowsUpdated = psStock.executeUpdate();

                    if (rowsUpdated == 0) {

                        conn.rollback();
                        return -1;
                    }

                    psTxn.setInt(1, item.getProductId());
                    psTxn.setInt(2, -item.getQuantity());
                    psTxn.setString(3, "Order #" + orderId);
                    psTxn.setString(4, LocalDateTime.now().toString());
                    psTxn.executeUpdate();
                }
            }

            conn.commit();
            return orderId;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return -1;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET order_status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markPaymentPaid(int orderId) {
        String sql = "UPDATE orders SET payment_status = 'Paid' WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int countAllOrders() {
        return singleIntQuery("SELECT COUNT(*) AS cnt FROM orders");
    }

    public double totalRevenue() {
        String sql = "SELECT COALESCE(SUM(total),0) AS rev FROM orders WHERE order_status != 'Cancelled'";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("rev");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countPendingCOD() {
        return singleIntQuery("SELECT COUNT(*) AS cnt FROM orders WHERE payment_method = 'COD' AND payment_status = 'Pending'");
    }

    private int singleIntQuery(String sql) {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setOrderDate(rs.getString("order_date"));
        o.setSubtotal(rs.getDouble("subtotal"));
        o.setDiscount(rs.getDouble("discount"));
        o.setTax(rs.getDouble("tax"));
        o.setDeliveryCharge(rs.getDouble("delivery_charge"));
        o.setTotal(rs.getDouble("total"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setOrderStatus(rs.getString("order_status"));
        return o;
    }
}

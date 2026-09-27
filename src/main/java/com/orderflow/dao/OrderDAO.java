package com.orderflow.dao;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.model.CustomerOrderCount;
import com.orderflow.model.MonthlyProfit;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.model.ProductSalesRow;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderDAO {

    private final ProductDAO productDAO = new ProductDAO();
    private final CapitalDAO capitalDAO = new CapitalDAO();

    /** Order statuses that are still allowed to be cancelled (before the order ships). */
    private static final List<String> CANCELLABLE_STATUSES = List.of("Pending", "Confirmed");

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

    public List<Order> findByCustomerId(int customerId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.name AS customer_name FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.id " +
                     "WHERE o.customer_id = ? ORDER BY o.id DESC";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
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

    /**
     * Creates a new order together with its items inside a single database
     * transaction, then reduces stock for every product sold and writes an
     * inventory_transactions record for each -- this mirrors the
     * "Order Creation Procedure" described in the project specification.
     *
     * Returns the new order id, or -1 if the order could not be created
     * (e.g. because stock was insufficient).
     */
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
            String getCost = "SELECT purchase_price FROM products WHERE id = ?";

            double totalProfit = 0;

            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(reduceStock);
                 PreparedStatement psTxn = conn.prepareStatement(logTxn);
                 PreparedStatement psCost = conn.prepareStatement(getCost)) {

                for (OrderItem item : items) {
                    psItem.setInt(1, orderId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getUnitPrice());
                    psItem.setDouble(5, item.getLineTotal());
                    psItem.executeUpdate();

                    // Only reduces stock if enough is available (WHERE stock_qty >= ?)
                    psStock.setInt(1, item.getQuantity());
                    psStock.setInt(2, item.getProductId());
                    psStock.setInt(3, item.getQuantity());
                    int rowsUpdated = psStock.executeUpdate();

                    if (rowsUpdated == 0) {
                        // Not enough stock for this product -> abort the whole order
                        conn.rollback();
                        return -1;
                    }

                    psTxn.setInt(1, item.getProductId());
                    psTxn.setInt(2, -item.getQuantity());
                    psTxn.setString(3, "Order #" + orderId);
                    psTxn.setString(4, LocalDateTime.now().toString());
                    psTxn.executeUpdate();

                    psCost.setInt(1, item.getProductId());
                    try (ResultSet costRs = psCost.executeQuery()) {
                        if (costRs.next()) {
                            double purchasePrice = costRs.getDouble("purchase_price");
                            totalProfit += item.getLineTotal() - (purchasePrice * item.getQuantity());
                        }
                    }
                }
            }

            // Non-COD payments are collected immediately at checkout, so the
            // profit on this sale becomes available capital right away. COD
            // orders stay "Pending" here and their profit is recorded later,
            // once the cash is actually collected on delivery (markPaymentPaid).
            if ("Paid".equals(order.getPaymentStatus())) {
                capitalDAO.addEntry(conn, totalProfit, "Sales Profit", "Profit from Order #" + orderId);
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

    /**
     * When COD cash is collected on delivery, payment status flips to Paid.
     * Business rule: a COD order can never be marked Paid before it has
     * actually been Delivered (or Completed). Returns false (no change) if
     * that rule would be violated, if the order is already Paid, or if the
     * order does not exist.
     */
    public boolean markPaymentPaid(int orderId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String currentStatus = null;
            String currentPayment = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_status, payment_status FROM orders WHERE id = ?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("order_status");
                        currentPayment = rs.getString("payment_status");
                    }
                }
            }

            if (currentStatus == null) {
                conn.rollback();
                return false; // order not found
            }
            if ("Paid".equals(currentPayment)) {
                conn.rollback();
                return false; // already paid, nothing to do
            }
            if (!"Delivered".equals(currentStatus) && !"Completed".equals(currentStatus)) {
                conn.rollback();
                return false; // cannot be marked Paid before it is Delivered
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET payment_status = 'Paid' WHERE id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // The profit on this order becomes available capital now that the cash was collected.
            double profit = 0;
            String profitSql = "SELECT oi.quantity, oi.line_total, p.purchase_price " +
                    "FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(profitSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profit += rs.getDouble("line_total") - (rs.getDouble("purchase_price") * rs.getInt("quantity"));
                    }
                }
            }
            capitalDAO.addEntry(conn, profit, "Sales Profit", "Profit from Order #" + orderId);

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

    /**
     * Cancels an order (only allowed before it has shipped) and restores the
     * stock that had been reserved for it. If the order had already been
     * paid (e.g. Card/Mobile Banking, charged immediately at checkout), the
     * profit that was recorded for it is reversed out of capital and the
     * payment status becomes "Refunded".
     */
    public boolean cancelOrder(int orderId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String currentStatus = null;
            String currentPayment = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_status, payment_status FROM orders WHERE id = ?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("order_status");
                        currentPayment = rs.getString("payment_status");
                    }
                }
            }

            if (currentStatus == null || !CANCELLABLE_STATUSES.contains(currentStatus)) {
                conn.rollback();
                return false; // already shipped/delivered/cancelled, or not found
            }

            // Restore stock for every item on this order
            String itemsSql = "SELECT product_id, quantity, line_total FROM order_items WHERE order_id = ?";
            try (PreparedStatement psItems = conn.prepareStatement(itemsSql);
                 PreparedStatement psStock = conn.prepareStatement(
                         "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?");
                 PreparedStatement psTxn = conn.prepareStatement(
                         "INSERT INTO inventory_transactions(product_id, change_qty, reason, transaction_date) VALUES (?,?,?,?)")) {
                psItems.setInt(1, orderId);
                try (ResultSet rs = psItems.executeQuery()) {
                    while (rs.next()) {
                        int productId = rs.getInt("product_id");
                        int qty = rs.getInt("quantity");

                        psStock.setInt(1, qty);
                        psStock.setInt(2, productId);
                        psStock.executeUpdate();

                        psTxn.setInt(1, productId);
                        psTxn.setInt(2, qty);
                        psTxn.setString(3, "Cancelled Order #" + orderId);
                        psTxn.setString(4, LocalDateTime.now().toString());
                        psTxn.executeUpdate();
                    }
                }
            }

            // If this order had already been paid (charged instantly at checkout),
            // reverse its profit out of capital since the sale no longer happened.
            String newPaymentStatus = currentPayment;
            if ("Paid".equals(currentPayment)) {
                double profit = 0;
                String profitSql = "SELECT oi.quantity, oi.line_total, p.purchase_price " +
                        "FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(profitSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            profit += rs.getDouble("line_total") - (rs.getDouble("purchase_price") * rs.getInt("quantity"));
                        }
                    }
                }
                capitalDAO.addEntry(conn, -profit, "Sales Refund", "Refund - Cancelled Order #" + orderId);
                newPaymentStatus = "Refunded";
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET order_status = 'Cancelled', payment_status = ? WHERE id = ?")) {
                ps.setString(1, newPaymentStatus);
                ps.setInt(2, orderId);
                ps.executeUpdate();
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

    public boolean isCancellable(String orderStatus) {
        return CANCELLABLE_STATUSES.contains(orderStatus);
    }

    // ---------- Dashboard / report helper queries ----------

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

    /** Per-product quantity sold and revenue generated within a date range (cancelled orders excluded). */
    public List<ProductSalesRow> productSalesBetween(String fromDate, String toDate) {
        List<ProductSalesRow> list = new ArrayList<>();
        String sql = "SELECT p.name AS product_name, SUM(oi.quantity) AS qty, SUM(oi.line_total) AS revenue " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE o.order_status != 'Cancelled' AND date(o.order_date) BETWEEN date(?) AND date(?) " +
                "GROUP BY p.id ORDER BY revenue DESC";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductSalesRow(rs.getString("product_name"), rs.getInt("qty"), rs.getDouble("revenue")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** The customer who has placed the most orders (cancelled orders excluded). */
    public CustomerOrderCount bestCustomerByOrderCount() {
        String sql = "SELECT c.name AS customer_name, COUNT(o.id) AS order_count " +
                "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                "WHERE o.order_status != 'Cancelled' " +
                "GROUP BY c.id ORDER BY order_count DESC LIMIT 1";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return new CustomerOrderCount(rs.getString("customer_name"), rs.getInt("order_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Monthly profit (sell price - buy price) for the last {@code monthsBack}
     * calendar months that have at least one order, oldest first. Stock that
     * has not been sold yet is intentionally not counted.
     */
    public List<MonthlyProfit> monthlyProfit(int monthsBack) {
        Map<String, Double> byMonth = new LinkedHashMap<>();
        String sql = "SELECT strftime('%Y-%m', o.order_date) AS ym, " +
                "SUM(oi.line_total - (p.purchase_price * oi.quantity)) AS profit " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE o.order_status != 'Cancelled' " +
                "GROUP BY ym ORDER BY ym DESC LIMIT ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, monthsBack);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byMonth.put(rs.getString("ym"), rs.getDouble("profit"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        List<MonthlyProfit> list = new ArrayList<>();
        // byMonth was filled newest-first (DESC); reverse it so the chart reads oldest -> newest
        List<String> months = new ArrayList<>(byMonth.keySet());
        for (int i = months.size() - 1; i >= 0; i--) {
            String month = months.get(i);
            list.add(new MonthlyProfit(month, byMonth.get(month)));
        }
        return list;
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

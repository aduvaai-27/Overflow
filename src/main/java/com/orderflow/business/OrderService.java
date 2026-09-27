package com.orderflow.business;

import com.orderflow.dao.OrderDAO;
import com.orderflow.model.CustomerOrderCount;
import com.orderflow.model.MonthlyProfit;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.model.ProductSalesRow;

import java.util.List;

public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();

    public List<Order> findAll() {
        return orderDAO.findAll();
    }

    public List<Order> findBetweenDates(String fromDate, String toDate) {
        return orderDAO.findBetweenDates(fromDate, toDate);
    }

    public List<Order> findByCustomerId(int customerId) {
        return orderDAO.findByCustomerId(customerId);
    }

    public List<OrderItem> findItemsByOrderId(int orderId) {
        return orderDAO.findItemsByOrderId(orderId);
    }

    public int createOrder(Order order, List<OrderItem> items) {
        return orderDAO.createOrder(order, items);
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        return orderDAO.updateOrderStatus(orderId, newStatus);
    }

    public boolean markPaymentPaid(int orderId) {
        return orderDAO.markPaymentPaid(orderId);
    }

    public boolean cancelOrder(int orderId) {
        return orderDAO.cancelOrder(orderId);
    }

    public boolean isCancellable(String orderStatus) {
        return orderDAO.isCancellable(orderStatus);
    }

    public int countAllOrders() {
        return orderDAO.countAllOrders();
    }

    public double totalRevenue() {
        return orderDAO.totalRevenue();
    }

    public int countPendingCOD() {
        return orderDAO.countPendingCOD();
    }

    public List<ProductSalesRow> productSalesBetween(String fromDate, String toDate) {
        return orderDAO.productSalesBetween(fromDate, toDate);
    }

    public CustomerOrderCount bestCustomerByOrderCount() {
        return orderDAO.bestCustomerByOrderCount();
    }

    public List<MonthlyProfit> monthlyProfit(int monthsBack) {
        return orderDAO.monthlyProfit(monthsBack);
    }
}

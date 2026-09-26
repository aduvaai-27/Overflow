package com.orderflow.controller;

import com.orderflow.dao.CustomerDAO;
import com.orderflow.dao.OrderDAO;
import com.orderflow.dao.ProductDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label totalProductsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label pendingCodLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @FXML
    public void initialize() {
        totalProductsLabel.setText(String.valueOf(productDAO.countActive()));
        totalCustomersLabel.setText(String.valueOf(customerDAO.countAll()));
        totalOrdersLabel.setText(String.valueOf(orderDAO.countAllOrders()));
        totalRevenueLabel.setText(String.format("%.2f", orderDAO.totalRevenue()));
        lowStockLabel.setText(String.valueOf(productDAO.countLowStock()));
        pendingCodLabel.setText(String.valueOf(orderDAO.countPendingCOD()));
    }
}

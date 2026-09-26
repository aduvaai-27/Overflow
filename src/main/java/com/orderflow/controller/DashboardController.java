package com.orderflow.controller;

import com.orderflow.dao.CustomerDAO;
import com.orderflow.dao.OrderDAO;
import com.orderflow.dao.ProductDAO;
import com.orderflow.service.ExchangeRateService;
import com.orderflow.service.StockAlertMonitor;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController implements Disposable {

    @FXML private Label totalProductsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label pendingCodLabel;
    @FXML private Label exchangeRateLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    private final StockAlertMonitor stockAlertMonitor = new StockAlertMonitor();

    @FXML
    public void initialize() {
        loadStats();

        stockAlertMonitor.start(count -> {
            lowStockLabel.setText(String.valueOf(count));
        }, 8);
    }

    private void loadStats() {
        totalProductsLabel.setText(String.valueOf(productDAO.countActive()));
        totalCustomersLabel.setText(String.valueOf(customerDAO.countAll()));
        totalOrdersLabel.setText(String.valueOf(orderDAO.countAllOrders()));
        totalRevenueLabel.setText(String.format("%.2f", orderDAO.totalRevenue()));
        pendingCodLabel.setText(String.valueOf(orderDAO.countPendingCOD()));
    }

    @FXML
    private void handleRefreshRate() {
        exchangeRateLabel.setText("Fetching latest rate...");

        Task<Double> task = exchangeRateService.fetchUsdToBdtRateTask();

        task.setOnSucceeded(e -> {
            double rate = task.getValue();
            exchangeRateLabel.setText(String.format("1 USD = %.2f BDT (fetched just now)", rate));
        });

        task.setOnFailed(e -> {
            exchangeRateLabel.setText("Could not fetch exchange rate. Check your internet connection.");
        });

        Thread apiThread = new Thread(task, "exchange-rate-api-thread");
        apiThread.setDaemon(true);
        apiThread.start();
    }

    @Override
    public void dispose() {
        stockAlertMonitor.stop();
    }
}

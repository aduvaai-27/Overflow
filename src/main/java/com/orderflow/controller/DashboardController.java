package com.orderflow.controller;

import com.orderflow.dao.CapitalDAO;
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
    @FXML private Label totalRevenueUsdLabel;
    @FXML private Label capitalLabel;
    @FXML private Label capitalUsdLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label pendingCodLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final CapitalDAO capitalDAO = new CapitalDAO();
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    private final StockAlertMonitor stockAlertMonitor = new StockAlertMonitor();

    private double lastKnownRevenue;
    private double lastKnownCapital;

    @FXML
    public void initialize() {
        loadStats();
        loadUsdEquivalents();

        stockAlertMonitor.start(count -> {
            lowStockLabel.setText(String.valueOf(count));
        }, 8);
    }

    private void loadStats() {
        totalProductsLabel.setText(String.valueOf(productDAO.countActive()));
        totalCustomersLabel.setText(String.valueOf(customerDAO.countAll()));
        totalOrdersLabel.setText(String.valueOf(orderDAO.countAllOrders()));
        lastKnownRevenue = orderDAO.totalRevenue();
        lastKnownCapital = capitalDAO.getCurrentCapital();
        totalRevenueLabel.setText(String.format("%.2f", lastKnownRevenue));
        capitalLabel.setText(String.format("%.2f", lastKnownCapital));
        pendingCodLabel.setText(String.valueOf(orderDAO.countPendingCOD()));
    }

    private void loadUsdEquivalents() {
        totalRevenueUsdLabel.setText("converting to USD...");
        capitalUsdLabel.setText("converting to USD...");

        Task<Double> rateTask = exchangeRateService.fetchUsdToBdtRateTask();

        rateTask.setOnSucceeded(e -> {
            double rate = rateTask.getValue();
            totalRevenueUsdLabel.setText(String.format("\u2248 $%.2f USD", lastKnownRevenue / rate));
            capitalUsdLabel.setText(String.format("\u2248 $%.2f USD", lastKnownCapital / rate));
        });

        rateTask.setOnFailed(e -> {
            totalRevenueUsdLabel.setText("USD rate unavailable");
            capitalUsdLabel.setText("USD rate unavailable");
        });

        Thread apiThread = new Thread(rateTask, "exchange-rate-api-thread");
        apiThread.setDaemon(true);
        apiThread.start();
    }

    @Override
    public void dispose() {
        stockAlertMonitor.stop();
    }
}

package com.orderflow.controller;

import com.orderflow.business.CapitalService;
import com.orderflow.business.CustomerService;
import com.orderflow.business.OrderService;
import com.orderflow.business.ProductService;
import com.orderflow.model.CustomerOrderCount;
import com.orderflow.model.MonthlyProfit;
import com.orderflow.service.ExchangeRateService;
import com.orderflow.service.StockAlertMonitor;
import com.orderflow.util.ChartUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.List;

public class DashboardController implements Disposable {

    private static final int PROFIT_CHART_MONTHS = 6;

    @FXML private Label totalProductsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalRevenueUsdLabel;
    @FXML private Label capitalLabel;
    @FXML private Label capitalUsdLabel;
    @FXML private Label bestCustomerLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label pendingCodLabel;
    @FXML private BarChart<String, Number> monthlyProfitChart;
    @FXML private BarChart<String, Number> comparisonChart;

    private final ProductService productService = new ProductService();
    private final CustomerService customerService = new CustomerService();
    private final OrderService orderService = new OrderService();
    private final CapitalService capitalService = new CapitalService();
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    // Background thread that keeps the low-stock counter fresh (Multithreading topic)
    private final StockAlertMonitor stockAlertMonitor = new StockAlertMonitor();

    private double lastKnownRevenue;
    private double lastKnownCapital;
    private double lastKnownProfit;

    @FXML
    public void initialize() {
        loadStats();
        monthlyProfitChart.setLegendVisible(false);
        ChartUtil.limitBarWidth(monthlyProfitChart, 90);
        ChartUtil.limitBarWidth(comparisonChart, 90);
        loadProfitChart();
        loadComparisonChart();
        loadUsdEquivalents();

        // Poll every 8 seconds on a background thread; UI is updated safely via Platform.runLater
        stockAlertMonitor.start(count -> {
            lowStockLabel.setText(String.valueOf(count));
        }, 8);
    }

    private void loadStats() {
        totalProductsLabel.setText(String.valueOf(productService.countActive()));
        totalCustomersLabel.setText(String.valueOf(customerService.countAll()));
        totalOrdersLabel.setText(String.valueOf(orderService.countAllOrders()));
        // Revenue = total sales value. Capital = the business's own money on hand.
        // These are intentionally two different numbers.
        lastKnownRevenue = orderService.totalRevenue();
        lastKnownCapital = capitalService.getCurrentCapital();
        totalRevenueLabel.setText(String.format("%.2f", lastKnownRevenue));
        capitalLabel.setText(String.format("%.2f", lastKnownCapital));
        pendingCodLabel.setText(String.valueOf(orderService.countPendingCOD()));

        CustomerOrderCount bestCustomer = orderService.bestCustomerByOrderCount();
        bestCustomerLabel.setText(bestCustomer == null
                ? "No orders yet"
                : bestCustomer.getCustomerName() + " (" + bestCustomer.getOrderCount() + " orders)");
    }

    /**
     * Fetches the live USD/BDT rate in the background (JSON Parsing and API
     * Response Handling topic) and, once it arrives, shows Total Revenue and
     * Capital as an approximate USD figure too - useful context, and never
     * blocks the rest of the Dashboard from loading while it's in flight.
     */
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

    private void loadProfitChart() {
        List<MonthlyProfit> monthly = orderService.monthlyProfit(PROFIT_CHART_MONTHS);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Profit (Tk)");
        lastKnownProfit = 0;
        for (MonthlyProfit mp : monthly) {
            series.getData().add(new XYChart.Data<>(mp.getMonth(), mp.getProfit()));
            lastKnownProfit += mp.getProfit();
        }
        monthlyProfitChart.getData().setAll(series);
        ChartUtil.colorizeByValue(series, "#2ecc71", "#e74c3c"); // green = profit, red = loss
    }

    private void loadComparisonChart() {
        comparisonChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tk");
        series.getData().add(new XYChart.Data<>("Revenue", lastKnownRevenue));
        series.getData().add(new XYChart.Data<>("Capital", lastKnownCapital));
        series.getData().add(new XYChart.Data<>("Profit", lastKnownProfit));
        comparisonChart.getData().setAll(series);
        ChartUtil.colorizeCategorical(series);
    }

    @Override
    public void dispose() {
        stockAlertMonitor.stop();
    }
}

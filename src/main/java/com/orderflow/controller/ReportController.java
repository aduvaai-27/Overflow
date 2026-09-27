package com.orderflow.controller;

import com.orderflow.business.OrderService;
import com.orderflow.model.Order;
import com.orderflow.model.ProductSalesRow;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.ChartUtil;
import com.orderflow.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportController {

    private static final int TOP_N = 6;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    @FXML private Label orderCountLabel;
    @FXML private Label revenueLabel;
    @FXML private Label avgOrderLabel;

    @FXML private TableView<Order> reportTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, Double> totalColumn;
    @FXML private TableColumn<Order, String> statusColumn;

    @FXML private BarChart<String, Number> topSellingChart;   // highest selling product (to customers), by quantity
    @FXML private BarChart<String, Number> topRevenueChart;   // highest revenue generating product

    private final OrderService orderService = new OrderService();
    private final ObservableList<Order> reportList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        dateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(DateUtil.formatForDisplay(cellData.getValue().getOrderDate())));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatus"));

        reportTable.setItems(reportList);

        topSellingChart.setLegendVisible(false);
        topRevenueChart.setLegendVisible(false);

        // Default range: last 30 days
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(30));

        handleGenerate();
    }

    @FXML
    private void handleGenerate() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            AlertUtil.warn("Validation", "Please choose both a start and an end date.");
            return;
        }
        if (from.isAfter(to)) {
            AlertUtil.warn("Validation", "The start date must be before the end date.");
            return;
        }

        List<Order> orders = orderService.findBetweenDates(from.toString(), to.toString());
        reportList.setAll(orders);

        double revenue = orders.stream()
                .filter(o -> !"Cancelled".equals(o.getOrderStatus()))
                .mapToDouble(Order::getTotal)
                .sum();

        orderCountLabel.setText(String.valueOf(orders.size()));
        revenueLabel.setText(String.format("%.2f", revenue));
        avgOrderLabel.setText(orders.isEmpty() ? "0.00" : String.format("%.2f", revenue / orders.size()));

        List<ProductSalesRow> sales = orderService.productSalesBetween(from.toString(), to.toString());
        renderTopSellingChart(sales);
        renderTopRevenueChart(sales);
    }

    private void renderTopSellingChart(List<ProductSalesRow> sales) {
        List<ProductSalesRow> topByQty = sales.stream()
                .sorted(Comparator.comparingInt(ProductSalesRow::getQuantitySold).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Units sold");
        for (ProductSalesRow row : topByQty) {
            series.getData().add(new XYChart.Data<>(row.getProductName(), row.getQuantitySold()));
        }
        topSellingChart.getData().setAll(series);
        ChartUtil.colorizeCategorical(series);
    }

    private void renderTopRevenueChart(List<ProductSalesRow> sales) {
        List<ProductSalesRow> topByRevenue = sales.stream()
                .sorted(Comparator.comparingDouble(ProductSalesRow::getRevenue).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (Tk)");
        for (ProductSalesRow row : topByRevenue) {
            series.getData().add(new XYChart.Data<>(row.getProductName(), row.getRevenue()));
        }
        topRevenueChart.getData().setAll(series);
        ChartUtil.colorizeCategorical(series);
    }
}

package com.orderflow.controller;

import com.orderflow.dao.OrderDAO;
import com.orderflow.model.Order;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class ReportController {

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

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Order> reportList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatus"));

        reportTable.setItems(reportList);

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

        List<Order> orders = orderDAO.findBetweenDates(from.toString(), to.toString());
        reportList.setAll(orders);

        double revenue = orders.stream()
                .filter(o -> !"Cancelled".equals(o.getOrderStatus()))
                .mapToDouble(Order::getTotal)
                .sum();

        orderCountLabel.setText(String.valueOf(orders.size()));
        revenueLabel.setText(String.format("%.2f", revenue));
        avgOrderLabel.setText(orders.isEmpty() ? "0.00" : String.format("%.2f", revenue / orders.size()));
    }
}

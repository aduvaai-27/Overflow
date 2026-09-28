package com.orderflow.controller;

import com.orderflow.business.CapitalService;
import com.orderflow.model.CapitalTransaction;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.DateUtil;
import com.orderflow.util.TableColorUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class CapitalController {

    @FXML private Label currentCapitalLabel;

    @FXML private TableView<CapitalTransaction> historyTable;
    @FXML private TableColumn<CapitalTransaction, Double> amountColumn;
    @FXML private TableColumn<CapitalTransaction, String> sourceColumn;
    @FXML private TableColumn<CapitalTransaction, String> reasonColumn;
    @FXML private TableColumn<CapitalTransaction, String> dateColumn;

    private final CapitalService capitalService = new CapitalService();
    private final ObservableList<CapitalTransaction> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColorUtil.colorizeSignedNumber(amountColumn, "#2ecc71", "#e74c3c");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        dateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(DateUtil.formatForDisplay(cellData.getValue().getTransactionDate())));
        historyTable.setItems(historyList);
        refresh();
    }

    private void refresh() {
        currentCapitalLabel.setText(String.format("Tk %.2f", capitalService.getCurrentCapital()));
        historyList.setAll(capitalService.findAll());
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleAddCapital() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Capital");
        dialog.setHeaderText("Add money to (or remove money from) your capital.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField amountField = new TextField();
        amountField.setPromptText("e.g. 5000 (use a negative number to withdraw)");
        TextField sourceField = new TextField();
        sourceField.setPromptText("e.g. Bank Loan, Personal Savings, Owner Withdrawal");
        TextField reasonField = new TextField();
        reasonField.setPromptText("e.g. Working capital top-up");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Amount (Tk):"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Source:"), 0, 1);
        grid.add(sourceField, 1, 1);
        grid.add(new Label("Reason:"), 0, 2);
        grid.add(reasonField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (amount == 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.warn("Validation", "Enter a non-zero amount.");
            return;
        }

        String source = sourceField.getText().trim();
        String reason = reasonField.getText().trim();
        if (source.isEmpty() || reason.isEmpty()) {
            AlertUtil.warn("Validation", "Please fill in both the source and the reason for this capital change.");
            return;
        }

        if (capitalService.addEntry(amount, source, reason)) {
            refresh();
        } else {
            AlertUtil.error("Error", "Could not save this capital entry.");
        }
    }
}

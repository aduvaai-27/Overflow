package com.orderflow.controller;

import com.orderflow.dao.OrderDAO;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class OrderController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, Double> totalColumn;
    @FXML private TableColumn<Order, String> paymentMethodColumn;
    @FXML private TableColumn<Order, String> paymentStatusColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;

    private final OrderDAO orderDAO = new OrderDAO();
    private final ObservableList<Order> orderList = FXCollections.observableArrayList();

    private static final List<String> STATUS_FLOW = List.of("Confirmed", "Shipped", "Delivered", "Completed");

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatus"));

        orderTable.setItems(orderList);
        refresh();
    }

    private void refresh() {
        orderList.setAll(orderDAO.findAll());
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleNewOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/orderflow/fxml/NewOrder.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.setTitle("New Order");
            dialog.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            var cssUrl = getClass().getResource("/com/orderflow/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Could not open the New Order window.");
        }
    }

    @FXML
    private void handleViewInvoice() {
        Order order = orderTable.getSelectionModel().getSelectedItem();
        if (order == null) {
            AlertUtil.warn("No selection", "Select an order first.");
            return;
        }

        List<OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        String invoiceText = buildInvoiceText(order, items);

        TextArea textArea = new TextArea(invoiceText);
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        textArea.setPrefSize(420, 420);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Invoice - Order #" + order.getId());
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(new VBox(textArea));
        alert.showAndWait();
    }

    private String buildInvoiceText(Order order, List<OrderItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("           ORDERFLOW\n");
        sb.append("            INVOICE\n");
        sb.append("------------------------------------------\n");
        sb.append("Invoice No : INV-").append(order.getId()).append("\n");
        sb.append("Order No   : ").append(order.getId()).append("\n");
        sb.append("Customer   : ").append(order.getCustomerName()).append("\n");
        sb.append("Date       : ").append(order.getOrderDate()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-20s %5s %8s %10s%n", "Product", "Qty", "Price", "Total"));
        sb.append("------------------------------------------\n");
        for (OrderItem item : items) {
            sb.append(String.format("%-20s %5d %8.2f %10.2f%n",
                    trim(item.getProductName(), 20), item.getQuantity(), item.getUnitPrice(), item.getLineTotal()));
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-27s%10.2f%n", "Subtotal:", order.getSubtotal()));
        sb.append(String.format("%-27s%10.2f%n", "Discount:", order.getDiscount()));
        sb.append(String.format("%-27s%10.2f%n", "Tax:", order.getTax()));
        sb.append(String.format("%-27s%10.2f%n", "Delivery:", order.getDeliveryCharge()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-27s%10.2f%n", "TOTAL (Tk):", order.getTotal()));
        sb.append("Payment Method : ").append(order.getPaymentMethod()).append("\n");
        sb.append("Payment Status : ").append(order.getPaymentStatus()).append("\n");
        sb.append("Order Status   : ").append(order.getOrderStatus()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("       Thank you for shopping with us!\n");
        return sb.toString();
    }

    private String trim(String text, int max) {
        return text.length() > max ? text.substring(0, max - 1) + "." : text;
    }

    @FXML
    private void handleAdvanceStatus() {
        Order order = orderTable.getSelectionModel().getSelectedItem();
        if (order == null) {
            AlertUtil.warn("No selection", "Select an order first.");
            return;
        }

        int currentIndex = STATUS_FLOW.indexOf(order.getOrderStatus());
        if (currentIndex == -1 || currentIndex == STATUS_FLOW.size() - 1) {
            AlertUtil.info("Order Status", "This order has no further status to advance to.");
            return;
        }

        String nextStatus = STATUS_FLOW.get(currentIndex + 1);
        orderDAO.updateOrderStatus(order.getId(), nextStatus);

        if (nextStatus.equals("Delivered") && "COD".equals(order.getPaymentMethod())) {
            orderDAO.markPaymentPaid(order.getId());
        }

        refresh();
    }

    @FXML
    private void handleMarkPaid() {
        Order order = orderTable.getSelectionModel().getSelectedItem();
        if (order == null) {
            AlertUtil.warn("No selection", "Select an order first.");
            return;
        }
        orderDAO.markPaymentPaid(order.getId());
        refresh();
    }
}

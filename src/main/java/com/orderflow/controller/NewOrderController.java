package com.orderflow.controller;

import com.orderflow.dao.CustomerDAO;
import com.orderflow.dao.OrderDAO;
import com.orderflow.dao.ProductDAO;
import com.orderflow.model.Customer;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.model.Product;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class NewOrderController {

    private static final double TAX_RATE = 0.05;      // fixed 5% tax, kept simple for a student project
    private static final double DELIVERY_CHARGE = 60;  // flat delivery charge

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private ComboBox<String> paymentMethodCombo;

    @FXML private TableView<OrderItem> cartTable;
    @FXML private TableColumn<OrderItem, String> productColumn;
    @FXML private TableColumn<OrderItem, Integer> qtyColumn;
    @FXML private TableColumn<OrderItem, Double> unitPriceColumn;
    @FXML private TableColumn<OrderItem, Double> lineTotalColumn;

    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label deliveryLabel;
    @FXML private Label totalLabel;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private final ObservableList<OrderItem> cart = FXCollections.observableArrayList();
    private boolean orderCreated = false;

    @FXML
    public void initialize() {
        customerCombo.setItems(FXCollections.observableArrayList(customerDAO.findAll()));
        productCombo.setItems(FXCollections.observableArrayList(productDAO.findAllActive()));

        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash on Delivery (COD)", "Card", "Mobile Banking"));
        paymentMethodCombo.getSelectionModel().selectFirst();

        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        lineTotalColumn.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        cartTable.setItems(cart);
        deliveryLabel.setText(String.format("%.2f", DELIVERY_CHARGE));
    }

    @FXML
    private void handleAddToCart() {
        Product product = productCombo.getValue();
        int qty = qtySpinner.getValue();

        if (product == null) {
            AlertUtil.warn("Validation", "Please choose a product.");
            return;
        }
        if (qty > product.getStockQty()) {
            AlertUtil.warn("Insufficient stock", "Only " + product.getStockQty() + " units of '" + product.getName() + "' are available.");
            return;
        }

        // If the product is already in the cart, just increase its quantity
        for (OrderItem item : cart) {
            if (item.getProductId() == product.getId()) {
                int newQty = item.getQuantity() + qty;
                if (newQty > product.getStockQty()) {
                    AlertUtil.warn("Insufficient stock", "Cannot add more than the available stock.");
                    return;
                }
                item.setQuantity(newQty);
                item.setLineTotal(newQty * item.getUnitPrice());
                cartTable.refresh();
                recalcTotals();
                return;
            }
        }

        cart.add(new OrderItem(product.getId(), product.getName(), qty, product.getSellingPrice()));
        recalcTotals();
    }

    @FXML
    private void handleRemoveItem() {
        OrderItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cart.remove(selected);
            recalcTotals();
        }
    }

    private void recalcTotals() {
        double subtotal = cart.stream().mapToDouble(OrderItem::getLineTotal).sum();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax + (cart.isEmpty() ? 0 : DELIVERY_CHARGE);

        subtotalLabel.setText(String.format("%.2f", subtotal));
        taxLabel.setText(String.format("%.2f", tax));
        totalLabel.setText(String.format("%.2f", total));
    }

    @FXML
    private void handleConfirm() {
        Customer customer = customerCombo.getValue();
        if (customer == null) {
            AlertUtil.warn("Validation", "Please choose a customer.");
            return;
        }
        if (cart.isEmpty()) {
            AlertUtil.warn("Validation", "Add at least one product to the cart.");
            return;
        }

        double subtotal = cart.stream().mapToDouble(OrderItem::getLineTotal).sum();
        double tax = subtotal * TAX_RATE;
        double delivery = DELIVERY_CHARGE;
        double total = subtotal + tax + delivery;

        String paymentMethodText = paymentMethodCombo.getValue();
        String paymentMethod = paymentMethodText.startsWith("Cash") ? "COD" : paymentMethodText;

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setSubtotal(subtotal);
        order.setDiscount(0);
        order.setTax(tax);
        order.setDeliveryCharge(delivery);
        order.setTotal(total);
        order.setPaymentMethod(paymentMethod);
        // COD stays "Pending" until delivery is completed; other methods are marked Paid immediately
        order.setPaymentStatus(paymentMethod.equals("COD") ? "Pending" : "Paid");
        order.setOrderStatus("Confirmed");

        List<OrderItem> items = new ArrayList<>(cart);
        int newOrderId = orderDAO.createOrder(order, items);

        if (newOrderId == -1) {
            AlertUtil.error("Order failed", "Could not create the order. Stock may have changed - please review quantities and try again.");
            return;
        }

        orderCreated = true;
        AlertUtil.info("Order Confirmed", "Order #" + newOrderId + " was created successfully.\nGrand Total: Tk " + String.format("%.2f", total));
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.close();
    }

    public boolean isOrderCreated() {
        return orderCreated;
    }
}

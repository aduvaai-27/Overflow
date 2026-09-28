package com.orderflow.controller;

import com.orderflow.business.CustomerService;
import com.orderflow.business.OrderService;
import com.orderflow.business.ProductService;
import com.orderflow.business.SupplierService;
import com.orderflow.model.Customer;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.model.Product;
import com.orderflow.model.Supplier;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewOrderController {

    private static final double TAX_RATE = 0.05;
    private static final double DELIVERY_CHARGE = 60;

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private Label productHintLabel;
    @FXML private VBox productDetailBox;
    @FXML private Label detailIdLabel;
    @FXML private Label detailPriceLabel;
    @FXML private Label detailStockLabel;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private ComboBox<String> paymentMethodCombo;

    @FXML private TableView<OrderItem> cartTable;
    @FXML private TableColumn<OrderItem, String> skuColumn;
    @FXML private TableColumn<OrderItem, String> productColumn;
    @FXML private TableColumn<OrderItem, String> supplierColumn;
    @FXML private TableColumn<OrderItem, Integer> qtyColumn;
    @FXML private TableColumn<OrderItem, Double> unitPriceColumn;
    @FXML private TableColumn<OrderItem, Double> lineTotalColumn;

    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label deliveryLabel;
    @FXML private Label totalLabel;

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();
    private final SupplierService supplierService = new SupplierService();
    private final OrderService orderService = new OrderService();

    private final ObservableList<OrderItem> cart = FXCollections.observableArrayList();
    private List<Product> allActiveProducts = List.of();
    private boolean orderCreated = false;

    @FXML
    public void initialize() {
        customerCombo.setItems(FXCollections.observableArrayList(customerService.findAll()));

        allActiveProducts = productService.findAllActive();
        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));
        productCombo.setCellFactory(list -> new ProductCell());
        productCombo.setButtonCell(new ProductCell());
        productCombo.setDisable(true);
        supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterProductsBySupplier(newVal));
        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> showProductDetails(newVal));

        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash on Delivery (COD)", "Card", "Mobile Banking"));
        paymentMethodCombo.getSelectionModel().selectFirst();

        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        supplierColumn.setCellValueFactory(cell -> {
            String name = cell.getValue().getSupplierName();
            return new javafx.beans.property.SimpleStringProperty(name == null ? "-" : name);
        });
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        lineTotalColumn.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        cartTable.setItems(cart);
        deliveryLabel.setText(String.format("%.2f", DELIVERY_CHARGE));
    }

    private void filterProductsBySupplier(Supplier supplier) {
        productCombo.getSelectionModel().clearSelection();
        if (supplier == null) {
            productCombo.setItems(FXCollections.observableArrayList());
            productCombo.setDisable(true);
            productHintLabel.setText("Choose a supplier to see its products.");
            return;
        }
        List<Product> filtered = allActiveProducts.stream()
                .filter(p -> p.getSupplierId() == supplier.getId())
                .collect(Collectors.toList());
        productCombo.setItems(FXCollections.observableArrayList(filtered));
        productCombo.setDisable(filtered.isEmpty());
        productHintLabel.setText(filtered.isEmpty()
                ? "No products from " + supplier.getName() + " yet."
                : filtered.size() + " product(s) from " + supplier.getName());
    }

    private void showProductDetails(Product product) {
        boolean show = product != null;
        productDetailBox.setVisible(show);
        productDetailBox.setManaged(show);
        if (!show) return;
        detailIdLabel.setText("Unique ID: " + product.getSku());
        detailPriceLabel.setText(String.format("Price: Tk %.2f", product.getSellingPrice()));
        detailStockLabel.setText("In stock: " + product.getStockQty() + "  |  Category: " + product.getCategoryName());
    }

    private static class ProductCell extends ListCell<Product> {
        @Override
        protected void updateItem(Product p, boolean empty) {
            super.updateItem(p, empty);
            setText(empty || p == null ? null
                    : String.format("%s  -  %s  (Tk %.2f)", p.getSku(), p.getName(), p.getSellingPrice()));
        }
    }

    @FXML
    private void handleAddToCart() {
        Supplier supplier = supplierCombo.getValue();
        Product product = productCombo.getValue();
        int qty = qtySpinner.getValue();

        if (supplier == null) {
            AlertUtil.warn("Validation", "Please choose a supplier first.");
            return;
        }
        if (product == null) {
            AlertUtil.warn("Validation", "Please choose a product.");
            return;
        }

        int alreadyInCart = cart.stream()
                .filter(i -> i.getProductId() == product.getId())
                .mapToInt(OrderItem::getQuantity).sum();
        if (alreadyInCart + qty > product.getStockQty()) {
            AlertUtil.warn("Insufficient stock", "Only " + product.getStockQty() + " units of '" + product.getName()
                    + "' (" + product.getSku() + ") are available"
                    + (alreadyInCart > 0 ? " - " + alreadyInCart + " already in the cart." : "."));
            return;
        }

        for (OrderItem item : cart) {
            if (item.getProductId() == product.getId() && item.getSupplierId() == supplier.getId()) {
                int newQty = item.getQuantity() + qty;
                item.setQuantity(newQty);
                item.setLineTotal(newQty * item.getUnitPrice());
                cartTable.refresh();
                recalcTotals();
                return;
            }
        }

        cart.add(new OrderItem(product.getId(), product.getName(), product.getSku(),
                supplier.getId(), supplier.getName(), qty, product.getSellingPrice()));
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
        order.setPaymentStatus(paymentMethod.equals("COD") ? "Pending" : "Paid");
        order.setOrderStatus("Confirmed");

        List<OrderItem> items = new ArrayList<>(cart);
        int newOrderId = orderService.createOrder(order, items);

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

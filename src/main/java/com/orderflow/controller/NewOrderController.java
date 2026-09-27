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
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewOrderController {

    private static final double TAX_RATE = 0.05;      // fixed 5% tax, kept simple for a student project
    private static final double DELIVERY_CHARGE = 60;  // flat delivery charge

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private Label productHintLabel;
    @FXML private Label productCodeLabel;
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

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();
    private final SupplierService supplierService = new SupplierService();
    private final OrderService orderService = new OrderService();

    private final ObservableList<OrderItem> cart = FXCollections.observableArrayList();
    private List<Product> allActiveProducts = List.of();
    private boolean orderCreated = false;

    // Guards against the supplier <-> product listeners re-triggering each other
    // when one field is filled in programmatically as a result of the other.
    private boolean syncingSelection = false;

    @FXML
    public void initialize() {
        customerCombo.setItems(FXCollections.observableArrayList(customerService.findAll()));
        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));
        allActiveProducts = productService.findAllActive();

        // Two ways to add an item, both valid:
        //   1) Select a supplier first -> the product list narrows to only that
        //      supplier's own listings.
        //   2) Select a product first -> the full catalogue is shown, and once a
        //      product is picked its owning supplier is filled in automatically,
        //      so the supplier never has to be chosen by hand in this flow.
        productCombo.setItems(FXCollections.observableArrayList(allActiveProducts));

        supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingSelection) return;
            filterProductsBySupplier(newVal);
        });
        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingSelection) return;
            handleProductPickedDirectly(newVal);
        });

        updateProductCodeLabel(null);

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

    /**
     * Narrows the product list down to only the products owned by the chosen supplier.
     * Runs only when the supplier is picked directly by the person (not when it was
     * auto-filled after a product was chosen first - see handleProductPickedDirectly).
     */
    private void filterProductsBySupplier(Supplier supplier) {
        syncingSelection = true;
        productCombo.getSelectionModel().clearSelection();
        syncingSelection = false;

        if (supplier == null) {
            productCombo.setItems(FXCollections.observableArrayList(allActiveProducts));
            productCombo.setDisable(false);
            productCombo.setPromptText("Select a product");
            productHintLabel.setText("Select a supplier to see only their products, or select a product to have its supplier filled in automatically.");
            updateProductCodeLabel(null);
            return;
        }
        List<Product> filtered = allActiveProducts.stream()
                .filter(p -> p.getSupplierId() != null && p.getSupplierId() == supplier.getId())
                .collect(Collectors.toList());
        productCombo.setItems(FXCollections.observableArrayList(filtered));
        productCombo.setDisable(filtered.isEmpty());
        productCombo.setPromptText(filtered.isEmpty() ? "No products available" : "Select a product");
        productHintLabel.setText(filtered.isEmpty()
                ? "No products are currently listed under " + supplier.getName() + "."
                : filtered.size() + " product(s) available from " + supplier.getName() + ".");
        updateProductCodeLabel(null);
    }

    /**
     * Called when the person picks a product directly, before choosing a supplier.
     * The product's own owning supplier is looked up and filled in automatically,
     * so the person never has to also state the supplier name in this flow.
     */
    private void handleProductPickedDirectly(Product product) {
        if (product == null) {
            updateProductCodeLabel(null);
            return;
        }
        updateProductCodeLabel(product);

        Supplier currentSupplier = supplierCombo.getValue();
        boolean alreadyMatches = currentSupplier != null && product.getSupplierId() != null
                && currentSupplier.getId() == product.getSupplierId();
        if (alreadyMatches) {
            return; // came from the supplier-first flow already - nothing to sync
        }

        Supplier owner = supplierCombo.getItems().stream()
                .filter(s -> product.getSupplierId() != null && s.getId() == product.getSupplierId())
                .findFirst()
                .orElse(null);

        syncingSelection = true;
        supplierCombo.setValue(owner);
        syncingSelection = false;

        productHintLabel.setText(owner != null
                ? "Supplier auto-selected: " + owner.getName() + "."
                : "This product has no assigned supplier yet, so it can't be purchased.");
    }

    /** Shows the unique product code (SKU) for whichever product is currently selected. */
    private void updateProductCodeLabel(Product product) {
        productCodeLabel.setText(product == null
                ? "Product Code: \u2014"
                : "Product Code: " + product.getSku());
    }

    @FXML
    private void handleAddToCart() {
        Supplier supplier = supplierCombo.getValue();
        Product product = productCombo.getValue();
        int qty = qtySpinner.getValue();

        if (product == null) {
            AlertUtil.warn("Validation", "Please select a product to add.");
            return;
        }
        if (supplier == null) {
            AlertUtil.warn("Validation", "This product has no supplier assigned yet, so it can't be purchased.");
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

        String displayName = product.getName() + "  \u2014  " + supplier.getName();
        cart.add(new OrderItem(product.getId(), displayName, qty, product.getSellingPrice()));
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

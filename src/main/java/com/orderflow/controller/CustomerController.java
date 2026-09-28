package com.orderflow.controller;

import com.orderflow.business.CustomerService;
import com.orderflow.business.OrderService;
import com.orderflow.model.Customer;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class CustomerController extends AbstractCrudController<Customer> {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField searchField;

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> addressColumn;

    private final CustomerService customerService = new CustomerService();
    private final OrderService orderService = new OrderService();
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

        customerTable.setItems(customerList);
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });

        // Click a customer's name/row to see their purchase history
        customerTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Customer> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    showHistory(row.getItem());
                }
            });
            return row;
        });

        refresh();
    }

    @Override
    protected void refresh() {
        customerList.setAll(customerService.findAll());
    }

    @Override
    protected void populateForm(Customer c) {
        selectedItem = c;
        nameField.setText(c.getName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        addressField.setText(c.getAddress());
    }

    @FXML
    private void handleAdd() {
        Customer c = buildFromForm(0);
        if (c == null) return;
        if (customerService.add(c)) {
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add customer.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a customer from the table first.");
            return;
        }
        Customer c = buildFromForm(selectedItem.getId());
        if (c == null) return;
        customerService.update(c);
        handleClear();
        refresh();
    }

    @FXML
    private void handleDelete() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a customer from the table first.");
            return;
        }
        if (AlertUtil.confirm("Confirm delete", "Delete customer '" + selectedItem.getName() + "'?")) {
            if (!customerService.delete(selectedItem.getId())) {
                AlertUtil.error("Error", "Could not delete this customer. They may already have existing orders.");
            }
            handleClear();
            refresh();
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @Override
    protected void clearForm() {
        selectedItem = null;
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        customerTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refresh();
        } else {
            customerList.setAll(customerService.search(keyword));
        }
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        refresh();
    }

    @FXML
    private void handleViewHistory() {
        Customer c = customerTable.getSelectionModel().getSelectedItem();
        if (c == null) {
            AlertUtil.warn("No selection", "Select a customer from the table first (or double-click their row).");
            return;
        }
        showHistory(c);
    }

    /** Shows what, when and how this customer has bought - as a stack of small receipt-style cards, one per order. */
    private void showHistory(Customer customer) {
        List<Order> orders = orderService.findByCustomerId(customer.getId());

        VBox container = new VBox(14);
        container.setPadding(new Insets(4));

        if (orders.isEmpty()) {
            Label empty = new Label("This customer has not placed any orders yet.");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 20;");
            container.getChildren().add(empty);
        } else {
            for (Order o : orders) {
                container.getChildren().add(buildReceiptCard(o));
            }
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(440, 480);
        scrollPane.setStyle("-fx-background-color: transparent;");

        String subtitle = (customer.getPhone() == null || customer.getPhone().isBlank())
                ? customer.getName()
                : customer.getName() + " (" + customer.getPhone() + ")";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Purchase History");
        alert.setHeaderText(subtitle);
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }

    /** One order rendered like a small printed receipt/bill card. */
    private VBox buildReceiptCard(Order order) {
        List<OrderItem> items = orderService.findItemsByOrderId(order.getId());

        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dcdfe3; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-font-family: 'Consolas', 'Courier New', monospace;");

        // Header row: order number (left) and date (right)
        Label orderNoLabel = new Label("Order #" + order.getId());
        orderNoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label dateLabel = new Label(DateUtil.formatForDisplay(order.getOrderDate()));
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        HBox header = row(orderNoLabel, dateLabel);

        // Payment / status line
        Label metaLabel = new Label(order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")  \u2022  "
                + order.getOrderStatus());
        metaLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        card.getChildren().addAll(header, metaLabel, new Separator());

        for (OrderItem item : items) {
            Label itemLabel = new Label(item.getQuantity() + " x " + item.getProductName()
                    + (item.getSku() == null ? "" : " [" + item.getSku() + "]"));
            itemLabel.setStyle("-fx-font-size: 12px;");
            Label lineTotalLabel = new Label(String.format("Tk %.2f", item.getLineTotal()));
            lineTotalLabel.setStyle("-fx-font-size: 12px;");
            card.getChildren().add(row(itemLabel, lineTotalLabel));
        }

        card.getChildren().add(new Separator());

        Label totalCaption = new Label("Total");
        totalCaption.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label totalValue = new Label(String.format("Tk %.2f", order.getTotal()));
        totalValue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        card.getChildren().add(row(totalCaption, totalValue));

        return card;
    }

    /** A left label and a right label on the same line, with a flexible gap between them. */
    private HBox row(Label left, Label right) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(left, spacer, right);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Customer buildFromForm(int id) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Customer name is required.");
            return null;
        }
        return new Customer(id, name, phoneField.getText().trim(), emailField.getText().trim(), addressField.getText().trim());
    }
}

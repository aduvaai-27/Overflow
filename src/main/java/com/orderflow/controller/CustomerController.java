package com.orderflow.controller;

import com.orderflow.dao.CustomerDAO;
import com.orderflow.model.Customer;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

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

    private final CustomerDAO customerDAO = new CustomerDAO();
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
            if (newVal != null) select(newVal);
        });

        refresh();
    }

    @Override
    protected void refresh() {
        customerList.setAll(customerDAO.findAll());
    }

    @Override
    protected void populateForm(Customer c) {
        nameField.setText(c.getName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        addressField.setText(c.getAddress());
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
    private void handleAdd() {
        Customer c = buildFromForm(0);
        if (c == null) return;
        if (customerDAO.add(c)) {
            clearForm();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add customer.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (!hasSelection()) {
            requireSelection();
            return;
        }
        Customer c = buildFromForm(selectedItem.getId());
        if (c == null) return;
        customerDAO.update(c);
        clearForm();
        refresh();
    }

    @FXML
    private void handleDelete() {
        if (!hasSelection()) {
            requireSelection();
            return;
        }
        if (confirmDelete(selectedItem.getName())) {
            if (!customerDAO.delete(selectedItem.getId())) {
                AlertUtil.error("Error", "Could not delete this customer. They may already have existing orders.");
            }
            clearForm();
            refresh();
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refresh();
        } else {
            customerList.setAll(customerDAO.search(keyword));
        }
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        refresh();
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

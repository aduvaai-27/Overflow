package com.orderflow.controller;

import com.orderflow.dao.CategoryDAO;
import com.orderflow.model.Category;
import com.orderflow.util.AlertUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class CategoryController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> idColumn;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TextField nameField;

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        categoryTable.setItems(categoryList);
        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) nameField.setText(newVal.getName());
        });

        refresh();
    }

    private void refresh() {
        categoryList.setAll(categoryDAO.findAll());
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        if (categoryDAO.add(name)) {
            nameField.clear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add category (it may already exist).");
        }
    }

    @FXML
    private void handleUpdate() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("No selection", "Select a category to update first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        categoryDAO.update(selected.getId(), name);
        refresh();
    }

    @FXML
    private void handleDelete() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("No selection", "Select a category to delete first.");
            return;
        }
        if (AlertUtil.confirm("Confirm delete", "Delete category '" + selected.getName() + "'?")) {
            if (!categoryDAO.delete(selected.getId())) {
                AlertUtil.error("Error", "Could not delete this category. It may still be used by existing products.");
            }
            refresh();
        }
    }
}

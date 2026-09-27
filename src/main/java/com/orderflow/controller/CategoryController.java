package com.orderflow.controller;

import com.orderflow.business.CategoryService;
import com.orderflow.model.Category;
import com.orderflow.util.AlertUtil;
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
    @FXML private TableColumn<Category, String> suppliersColumn;
    @FXML private TextField nameField;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private Category selectedCategory;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        suppliersColumn.setCellValueFactory(cellData -> {
            String suppliers = cellData.getValue().getSuppliersDisplay();
            return new javafx.beans.property.SimpleStringProperty(suppliers == null ? "-" : suppliers);
        });

        categoryTable.setItems(categoryList);
        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedCategory = newVal;
                nameField.setText(newVal.getName());
            }
        });

        refresh();
    }

    private void refresh() {
        categoryList.setAll(categoryService.findAll());
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        if (categoryService.add(name)) {
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add category (it may already exist).");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCategory == null) {
            AlertUtil.warn("No selection", "Select a category to update first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        categoryService.update(selectedCategory.getId(), name);
        handleClear();
        refresh();
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory == null) {
            AlertUtil.warn("No selection", "Select a category to delete first.");
            return;
        }
        if (AlertUtil.confirm("Confirm delete", "Delete category '" + selectedCategory.getName() + "'?")) {
            if (!categoryService.delete(selectedCategory.getId())) {
                AlertUtil.error("Error", "Could not delete this category. It may still be used by existing products.");
            }
            handleClear();
            refresh();
        }
    }

    @FXML
    private void handleClear() {
        selectedCategory = null;
        nameField.clear();
        categoryTable.getSelectionModel().clearSelection();
    }
}

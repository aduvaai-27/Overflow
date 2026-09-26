package com.orderflow.controller;

import com.orderflow.dao.CategoryDAO;
import com.orderflow.model.Category;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class CategoryController extends AbstractCrudController<Category> {

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
            if (newVal != null) select(newVal);
        });

        refresh();
    }

    @Override
    protected void refresh() {
        categoryList.setAll(categoryDAO.findAll());
    }

    @Override
    protected void populateForm(Category c) {
        nameField.setText(c.getName());
    }

    @Override
    protected void clearForm() {
        selectedItem = null;
        nameField.clear();
        categoryTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        if (categoryDAO.add(name)) {
            clearForm();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add category (it may already exist).");
        }
    }

    @FXML
    private void handleUpdate() {
        if (!hasSelection()) {
            requireSelection();
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        categoryDAO.update(selectedItem.getId(), name);
        refresh();
    }

    @FXML
    private void handleDelete() {
        if (!hasSelection()) {
            requireSelection();
            return;
        }
        if (confirmDelete(selectedItem.getName())) {
            if (!categoryDAO.delete(selectedItem.getId())) {
                AlertUtil.error("Error", "Could not delete this category. It may still be used by existing products.");
            }
            refresh();
        }
    }
}

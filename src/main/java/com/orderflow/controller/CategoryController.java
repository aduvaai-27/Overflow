package com.orderflow.controller;

import com.orderflow.business.CategoryService;
import com.orderflow.model.CatalogRow;
import com.orderflow.model.Category;
import com.orderflow.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class CategoryController extends AbstractCrudController<Category> {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> idColumn;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> suppliersColumn;
    @FXML private TextField nameField;

    // Catalogue of the selected category: Supplier, Product, Price, Unique ID
    @FXML private Label catalogTitleLabel;
    @FXML private TableView<CatalogRow> catalogTable;
    @FXML private TableColumn<CatalogRow, String> catSupplierColumn;
    @FXML private TableColumn<CatalogRow, String> catProductColumn;
    @FXML private TableColumn<CatalogRow, Double> catPriceColumn;
    @FXML private TableColumn<CatalogRow, String> catSkuColumn;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private final ObservableList<CatalogRow> catalogList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        suppliersColumn.setCellValueFactory(cellData -> {
            String suppliers = cellData.getValue().getSuppliersDisplay();
            return new javafx.beans.property.SimpleStringProperty(suppliers == null ? "-" : suppliers);
        });

        catSupplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        catProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        catPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        catSkuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        catalogTable.setItems(catalogList);

        categoryTable.setItems(categoryList);
        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                select(newVal);
            }
            showCatalog(newVal);
        });

        refresh();
    }

    @Override
    protected void refresh() {
        categoryList.setAll(categoryService.findAll());
        showCatalog(null);
    }

    @Override
    protected void populateForm(Category category) {
        nameField.setText(category.getName());
    }

    /** Fills the Supplier / Product / Price / Unique ID table for the given category. */
    private void showCatalog(Category category) {
        if (category == null) {
            catalogList.clear();
            catalogTitleLabel.setText("Select a category to see its suppliers and products");
            return;
        }
        catalogList.setAll(categoryService.findCatalog(category.getId()));
        catalogTitleLabel.setText(category.getName() + " - suppliers and products");
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
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a category to update first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Please enter a category name.");
            return;
        }
        categoryService.update(selectedItem.getId(), name);
        handleClear();
        refresh();
    }

    @FXML
    private void handleDelete() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a category to delete first.");
            return;
        }
        if (AlertUtil.confirm("Confirm delete", "Delete category '" + selectedItem.getName() + "'?")) {
            if (!categoryService.delete(selectedItem.getId())) {
                AlertUtil.error("Error", "Could not delete this category. It may still be used by existing products.");
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
        categoryTable.getSelectionModel().clearSelection();
    }
}

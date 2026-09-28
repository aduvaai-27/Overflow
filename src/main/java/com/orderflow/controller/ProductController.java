package com.orderflow.controller;

import com.orderflow.business.CategoryService;
import com.orderflow.business.ProductService;
import com.orderflow.business.SupplierService;
import com.orderflow.model.Category;
import com.orderflow.model.Product;
import com.orderflow.model.Supplier;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.TableColorUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductController extends AbstractCrudController<Product> {

    @FXML private TextField nameField;
    @FXML private TextField skuField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField purchasePriceField;
    @FXML private TextField sellingPriceField;
    @FXML private TextField stockQtyField;
    @FXML private TextField minStockField;
    @FXML private TextField searchField;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> supplierColumn;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private TableColumn<Product, Double> purchasePriceColumn;
    @FXML private TableColumn<Product, Double> sellingPriceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, Integer> minStockColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final SupplierService supplierService = new SupplierService();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        supplierColumn.setCellValueFactory(cellData -> {
            String supplierName = cellData.getValue().getSupplierName();
            return new javafx.beans.property.SimpleStringProperty(supplierName == null || supplierName.isBlank() ? "-" : supplierName);
        });
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStock"));

        statusColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            String text = !p.isActive() ? "Inactive" : (p.isLowStock() ? "LOW STOCK" : "OK");
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        TableColorUtil.colorizeText(statusColumn, value -> switch (value) {
            case "OK" -> "#2ecc71";
            case "LOW STOCK" -> "#e74c3c";
            default -> "#95a5a6";
        });

        categoryCombo.setItems(FXCollections.observableArrayList(categoryService.findAll()));
        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));

        productTable.setItems(productList);
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });

        refresh();
    }

    @Override
    protected void refresh() {
        productList.setAll(productService.findAll());
    }

    @Override
    protected void populateForm(Product p) {
        selectedItem = p;
        nameField.setText(p.getName());
        skuField.setText(p.getSku());
        purchasePriceField.setText(String.valueOf(p.getPurchasePrice()));
        sellingPriceField.setText(String.valueOf(p.getSellingPrice()));
        stockQtyField.setText(String.valueOf(p.getStockQty()));
        minStockField.setText(String.valueOf(p.getMinStock()));
        for (Category c : categoryCombo.getItems()) {
            if (c.getId() == p.getCategoryId()) {
                categoryCombo.getSelectionModel().select(c);
                break;
            }
        }
        supplierCombo.getSelectionModel().clearSelection();
        for (Supplier sup : supplierCombo.getItems()) {
            if (sup.getId() == p.getSupplierId()) {
                supplierCombo.getSelectionModel().select(sup);
                break;
            }
        }
    }

    @FXML
    private void handleAdd() {
        Product p = buildProductFromForm(null);
        if (p == null) return;

        if (productService.add(p)) {
            AlertUtil.info("Success", "Product added successfully.\nUnique ID: " + p.getSku());
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add product. Check that the Unique ID is not already used.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a product from the table first.");
            return;
        }
        Product p = buildProductFromForm(selectedItem.getId());
        if (p == null) return;
        p.setActive(selectedItem.isActive());

        if (productService.update(p)) {
            AlertUtil.info("Success", "Product updated successfully.");
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not update product.");
        }
    }

    @FXML
    private void handleDeactivate() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a product from the table first.");
            return;
        }
        if (AlertUtil.confirm("Confirm", "Deactivate '" + selectedItem.getName() + "'? It will no longer appear for new orders.")) {
            productService.deactivate(selectedItem.getId());
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
        skuField.clear();
        purchasePriceField.clear();
        sellingPriceField.clear();
        stockQtyField.clear();
        minStockField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        productTable.getSelectionModel().clearSelection();
        supplierCombo.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refresh();
        } else {
            productList.setAll(productService.search(keyword));
        }
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        refresh();
    }

    /** Reads the form fields, validates them, and builds a Product object. Returns null if invalid. */
    private Product buildProductFromForm(Integer existingId) {
        String name = nameField.getText().trim();
        String sku = skuField.getText().trim();
        Category category = categoryCombo.getValue();
        Supplier supplier = supplierCombo.getValue();

        if (name.isEmpty() || category == null || supplier == null) {
            AlertUtil.warn("Validation", "Name, Category and Supplier are required.");
            return null;
        }
        // Every product must have a Unique ID: generate the next one if the field was left blank
        if (sku.isEmpty()) {
            sku = productService.generateUniqueId(category.getName());
        }

        double purchasePrice, sellingPrice;
        int stockQty, minStock;
        try {
            purchasePrice = Double.parseDouble(purchasePriceField.getText().trim());
            sellingPrice = Double.parseDouble(sellingPriceField.getText().trim());
            stockQty = Integer.parseInt(stockQtyField.getText().trim());
            minStock = Integer.parseInt(minStockField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.warn("Validation", "Price and stock fields must be valid numbers.");
            return null;
        }

        Product p = new Product();
        if (existingId != null) p.setId(existingId);
        p.setName(name);
        p.setSku(sku);
        p.setCategoryId(category.getId());
        p.setCategoryName(category.getName());
        p.setSupplierId(supplier.getId());
        p.setSupplierName(supplier.getName());
        p.setPurchasePrice(purchasePrice);
        p.setSellingPrice(sellingPrice);
        p.setStockQty(stockQty);
        p.setMinStock(minStock);
        p.setActive(true);
        return p;
    }
}

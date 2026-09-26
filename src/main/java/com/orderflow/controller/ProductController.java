package com.orderflow.controller;

import com.orderflow.dao.CategoryDAO;
import com.orderflow.dao.ProductDAO;
import com.orderflow.model.Category;
import com.orderflow.model.Product;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.TableColorUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductController {

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
    @FXML private TableColumn<Product, Double> purchasePriceColumn;
    @FXML private TableColumn<Product, Double> sellingPriceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, Integer> minStockColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();

    private Product selectedProduct;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
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

        categoryCombo.setItems(FXCollections.observableArrayList(categoryDAO.findAll()));

        productTable.setItems(productList);
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });

        refresh();
    }

    private void refresh() {
        productList.setAll(productDAO.findAll());
    }

    private void populateForm(Product p) {
        selectedProduct = p;
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
    }

    @FXML
    private void handleAdd() {
        Product p = buildProductFromForm(null);
        if (p == null) return;

        if (productDAO.add(p)) {
            AlertUtil.info("Success", "Product added successfully.");
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not add product. Check that the SKU is unique.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedProduct == null) {
            AlertUtil.warn("No selection", "Select a product from the table first.");
            return;
        }
        Product p = buildProductFromForm(selectedProduct.getId());
        if (p == null) return;
        p.setActive(selectedProduct.isActive());

        if (productDAO.update(p)) {
            AlertUtil.info("Success", "Product updated successfully.");
            handleClear();
            refresh();
        } else {
            AlertUtil.error("Error", "Could not update product.");
        }
    }

    @FXML
    private void handleDeactivate() {
        if (selectedProduct == null) {
            AlertUtil.warn("No selection", "Select a product from the table first.");
            return;
        }
        if (AlertUtil.confirm("Confirm", "Deactivate '" + selectedProduct.getName() + "'? It will no longer appear for new orders.")) {
            productDAO.deactivate(selectedProduct.getId());
            handleClear();
            refresh();
        }
    }

    @FXML
    private void handleClear() {
        selectedProduct = null;
        nameField.clear();
        skuField.clear();
        purchasePriceField.clear();
        sellingPriceField.clear();
        stockQtyField.clear();
        minStockField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        productTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refresh();
        } else {
            productList.setAll(productDAO.search(keyword));
        }
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        refresh();
    }

    private Product buildProductFromForm(Integer existingId) {
        String name = nameField.getText().trim();
        String sku = skuField.getText().trim();
        Category category = categoryCombo.getValue();

        if (name.isEmpty() || sku.isEmpty() || category == null) {
            AlertUtil.warn("Validation", "Name, SKU and Category are required.");
            return null;
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
        p.setPurchasePrice(purchasePrice);
        p.setSellingPrice(sellingPrice);
        p.setStockQty(stockQty);
        p.setMinStock(minStock);
        p.setActive(true);
        return p;
    }
}

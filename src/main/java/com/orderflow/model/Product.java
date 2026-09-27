package com.orderflow.model;

/**
 * Represents a product that OrderFlow sells and keeps stock of.
 * Each product row is owned by exactly one supplier - its own listing, with
 * its own stock and price. Two companies selling the same-named item (e.g.
 * both selling a "Cotton T-Shirt") are two separate Product rows that just
 * happen to share a name.
 */
public class Product {
    private int id;
    private String name;
    private String sku;
    private int categoryId;
    private String categoryName;
    private Integer supplierId;      // the one company this exact listing belongs to (nullable until assigned)
    private String supplierName;     // derived, for display
    private double purchasePrice;
    private double sellingPrice;
    private int stockQty;
    private int minStock;
    private boolean active;

    public Product() {}

    public Product(int id, String name, String sku, int categoryId, String categoryName,
                    double purchasePrice, double sellingPrice, int stockQty, int minStock, boolean active) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.stockQty = stockQty;
        this.minStock = minStock;
        this.active = active;
    }

    public boolean isLowStock() { return stockQty <= minStock; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return name + " (Stock: " + stockQty + ")"; }
}

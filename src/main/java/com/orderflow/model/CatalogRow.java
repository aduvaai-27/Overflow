package com.orderflow.model;

/** One line of the category catalogue: which supplier provides which product, at what price, under which Unique ID. */
public class CatalogRow {
    private final int supplierId;
    private final String supplierName;
    private final int productId;
    private final String productName;
    private final String sku;
    private final double price;

    public CatalogRow(int supplierId, String supplierName, int productId, String productName, String sku, double price) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.price = price;
    }

    public int getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public double getPrice() { return price; }
}

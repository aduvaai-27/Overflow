package com.orderflow.model;

/** One line item inside an order (a product + quantity). */
public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private String productName;
    private String sku;            // the product's Unique ID
    private int supplierId;        // supplier the item was picked from (0 = unknown / older orders)
    private String supplierName;
    private int quantity;
    private double unitPrice;
    private double lineTotal;

    public OrderItem() {}

    public OrderItem(int productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity * unitPrice;
    }

    public OrderItem(int productId, String productName, String sku, int supplierId, String supplierName,
                     int quantity, double unitPrice) {
        this(productId, productName, quantity, unitPrice);
        this.sku = sku;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}

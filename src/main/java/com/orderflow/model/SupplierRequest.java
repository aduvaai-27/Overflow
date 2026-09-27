package com.orderflow.model;

/**
 * A restock request sent to a supplier for a given product/quantity.
 * Moves through phases: Requested -> Shipped -> Completed. Reaching
 * Completed automatically increases the product's stock and pays the
 * supplier out of capital.
 */
public class SupplierRequest {
    private int id;
    private int supplierId;
    private String supplierName;
    private int productId;
    private String productName;
    private int quantity;
    private double unitCost;
    private String phase;
    private String requestDate;
    private String completedDate;

    public SupplierRequest() {}

    public double getTotalCost() { return quantity * unitCost; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
}

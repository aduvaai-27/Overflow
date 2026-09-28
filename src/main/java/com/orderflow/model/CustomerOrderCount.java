package com.orderflow.model;

public class CustomerOrderCount {
    private final String customerName;
    private final int orderCount;

    public CustomerOrderCount(String customerName, int orderCount) {
        this.customerName = customerName;
        this.orderCount = orderCount;
    }

    public String getCustomerName() { return customerName; }
    public int getOrderCount() { return orderCount; }
}

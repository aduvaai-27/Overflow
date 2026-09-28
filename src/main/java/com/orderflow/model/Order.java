package com.orderflow.model;

public class Order {
    private int id;
    private int customerId;
    private String customerName;
    private String orderDate;
    private double subtotal;
    private double discount;
    private double tax;
    private double deliveryCharge;
    private double total;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;

    public Order() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(double deliveryCharge) { this.deliveryCharge = deliveryCharge; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
}

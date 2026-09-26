package com.orderflow.model;

public class CapitalTransaction {
    private int id;
    private double amount;
    private String source;
    private String reason;
    private String transactionDate;

    public CapitalTransaction() {}

    public CapitalTransaction(int id, double amount, String source, String reason, String transactionDate) {
        this.id = id;
        this.amount = amount;
        this.source = source;
        this.reason = reason;
        this.transactionDate = transactionDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
}

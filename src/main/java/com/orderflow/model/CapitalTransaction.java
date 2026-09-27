package com.orderflow.model;

/**
 * One entry in the capital ledger. Capital is the business's own money and
 * is tracked separately from sales revenue: a positive amount increases
 * capital (manual top-up, or automatic sale profit); a negative amount
 * decreases it (e.g. automatically paying a supplier on a completed
 * restock request). "Source" is where the money came from/went to (e.g.
 * "Sales Profit", "Bank Loan"); "Reason" is the specific detail of why
 * (e.g. "Profit from Order #4", "Working capital top-up").
 */
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

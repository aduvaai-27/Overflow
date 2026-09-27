package com.orderflow.model;

/** Profit (sell price - buy price) for one calendar month, used by the Dashboard chart. */
public class MonthlyProfit {
    private final String month; // e.g. "2026-09"
    private final double profit;

    public MonthlyProfit(String month, double profit) {
        this.month = month;
        this.profit = profit;
    }

    public String getMonth() { return month; }
    public double getProfit() { return profit; }
}

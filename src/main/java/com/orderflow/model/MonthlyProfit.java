package com.orderflow.model;

public class MonthlyProfit {
    private final String month;
    private final double profit;

    public MonthlyProfit(String month, double profit) {
        this.month = month;
        this.profit = profit;
    }

    public String getMonth() { return month; }
    public double getProfit() { return profit; }
}

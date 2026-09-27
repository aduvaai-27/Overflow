package com.orderflow.model;

/** Represents a product category, e.g. Electronics, Clothing. */
public class Category {
    private int id;
    private String name;
    private String suppliersDisplay; // comma-separated supplier names, for display only (read-only, derived)

    public Category() {}

    public Category(int id, String name) {
        this(id, name, null);
    }

    public Category(int id, String name, String suppliersDisplay) {
        this.id = id;
        this.name = name;
        this.suppliersDisplay = suppliersDisplay;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSuppliersDisplay() { return suppliersDisplay; }
    public void setSuppliersDisplay(String suppliersDisplay) { this.suppliersDisplay = suppliersDisplay; }

    @Override
    public String toString() { return name; }
}

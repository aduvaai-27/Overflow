package com.orderflow.model;

/** Represents a product category, e.g. Electronics, Clothing. */
public class Category extends BaseEntity {
    private String name;
    private String suppliersDisplay;

    public Category() {}

    public Category(int id, String name) {
        this(id, name, null);
    }

    public Category(int id, String name, String suppliersDisplay) {
        setId(id);
        this.name = name;
        this.suppliersDisplay = suppliersDisplay;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSuppliersDisplay() { return suppliersDisplay; }
    public void setSuppliersDisplay(String suppliersDisplay) { this.suppliersDisplay = suppliersDisplay; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String toString() { return name; }
}

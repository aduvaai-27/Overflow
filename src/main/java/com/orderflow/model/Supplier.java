package com.orderflow.model;

/** Represents a supplier/company that products are purchased (restocked) from. */
public class Supplier {
    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String categoriesDisplay; // comma-separated category names this supplier provides, for display only

    public Supplier() {}

    public Supplier(int id, String name, String phone, String email, String address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCategoriesDisplay() { return categoriesDisplay; }
    public void setCategoriesDisplay(String categoriesDisplay) { this.categoriesDisplay = categoriesDisplay; }

    @Override
    public String toString() { return name; }
}

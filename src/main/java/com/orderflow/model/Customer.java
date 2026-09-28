package com.orderflow.model;

/** Represents a customer who places orders. */
public class Customer extends BaseEntity {
    private String name;
    private String phone;
    private String email;
    private String address;

    public Customer() {}

    public Customer(int id, String name, String phone, String email, String address) {
        setId(id);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String toString() { return name + " (" + phone + ")"; }
}

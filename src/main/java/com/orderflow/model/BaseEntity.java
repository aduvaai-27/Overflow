package com.orderflow.model;

/**
 * Topic: Advanced OOP (abstract classes, inheritance, polymorphism).
 *
 * Common parent of every database-backed model that has a primary key.
 * Category, Customer, Product and Supplier all extend it, so they share
 * one id field and must each say how they are shown to the user.
 */
public abstract class BaseEntity {

    private int id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    /** Short human-readable label; every subclass implements it its own way (polymorphism). */
    public abstract String getDisplayName();
}

package com.orderflow.controller;

import com.orderflow.model.BaseEntity;
import com.orderflow.util.AlertUtil;

/**
 * Topic: Advanced OOP (abstract class + generics + inheritance).
 *
 * Shared behaviour for every CRUD screen (Category, Customer, Product,
 * Supplier). Each subclass supplies its own refresh / populateForm /
 * clearForm; the "which row is selected" logic lives here once.
 */
public abstract class AbstractCrudController<T extends BaseEntity> {

    protected T selectedItem;

    protected abstract void refresh();

    protected abstract void populateForm(T item);

    protected abstract void clearForm();

    protected void select(T item) {
        selectedItem = item;
        populateForm(item);
    }

    protected boolean hasSelection() {
        return selectedItem != null;
    }

    protected void requireSelection() {
        AlertUtil.warn("No selection", "Select a row from the table first.");
    }

    protected boolean confirmDelete(String label) {
        return AlertUtil.confirm("Confirm delete", "Delete '" + label + "'?");
    }
}

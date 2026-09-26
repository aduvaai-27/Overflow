package com.orderflow.controller;

import com.orderflow.util.AlertUtil;

public abstract class AbstractCrudController<T> {

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

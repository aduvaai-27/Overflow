package com.orderflow.business;

import com.orderflow.dao.SupplierDAO;
import com.orderflow.model.Supplier;

import java.util.List;
import java.util.Set;

public class SupplierService {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    public List<Supplier> findAll() {
        return supplierDAO.findAll();
    }

    public Set<Integer> findCategoryIdsForSupplier(int supplierId) {
        return supplierDAO.findCategoryIdsForSupplier(supplierId);
    }

    public boolean add(Supplier s, Set<Integer> categoryIds) {
        return supplierDAO.add(s, categoryIds);
    }

    public boolean update(Supplier s, Set<Integer> categoryIds) {
        return supplierDAO.update(s, categoryIds);
    }

    public boolean delete(int id) {
        return supplierDAO.delete(id);
    }
}

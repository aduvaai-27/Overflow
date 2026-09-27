package com.orderflow.business;

import com.orderflow.dao.CategoryDAO;
import com.orderflow.model.Category;

import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Category> findAll() {
        return categoryDAO.findAll();
    }

    public boolean add(String name) {
        return categoryDAO.add(name);
    }

    public boolean update(int id, String name) {
        return categoryDAO.update(id, name);
    }

    public boolean delete(int id) {
        return categoryDAO.delete(id);
    }
}

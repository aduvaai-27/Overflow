package com.orderflow.business;

import com.orderflow.dao.ProductDAO;
import com.orderflow.model.Product;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();

    public List<Product> findAllActive() {
        return productDAO.findAllActive();
    }

    public List<Product> findAll() {
        return productDAO.findAll();
    }

    public List<Product> search(String keyword) {
        return productDAO.search(keyword);
    }

    public int countLowStock() {
        return productDAO.countLowStock();
    }

    public String generateUniqueId(String categoryName) {
        return productDAO.generateUniqueId(categoryName);
    }

    public int countBySupplier(int supplierId) {
        return productDAO.countBySupplier(supplierId);
    }

    public boolean add(Product p) {
        return productDAO.add(p);
    }

    public boolean update(Product p) {
        return productDAO.update(p);
    }

    public boolean deactivate(int id) {
        return productDAO.deactivate(id);
    }

    public boolean adjustStock(int productId, int deltaQty, String reason) {
        return productDAO.adjustStock(productId, deltaQty, reason);
    }

    public int countActive() {
        return productDAO.countActive();
    }
}

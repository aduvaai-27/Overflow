package com.orderflow.business;

import com.orderflow.dao.CustomerDAO;
import com.orderflow.model.Customer;

import java.util.List;

public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Customer> findAll() {
        return customerDAO.findAll();
    }

    public List<Customer> search(String keyword) {
        return customerDAO.search(keyword);
    }

    public boolean add(Customer c) {
        return customerDAO.add(c);
    }

    public boolean update(Customer c) {
        return customerDAO.update(c);
    }

    public boolean delete(int id) {
        return customerDAO.delete(id);
    }

    public int countAll() {
        return customerDAO.countAll();
    }
}

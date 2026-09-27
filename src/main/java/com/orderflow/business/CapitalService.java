package com.orderflow.business;

import com.orderflow.dao.CapitalDAO;
import com.orderflow.model.CapitalTransaction;

import java.util.List;

public class CapitalService {

    private final CapitalDAO capitalDAO = new CapitalDAO();

    public double getCurrentCapital() {
        return capitalDAO.getCurrentCapital();
    }

    public List<CapitalTransaction> findAll() {
        return capitalDAO.findAll();
    }

    public boolean addEntry(double amount, String source, String reason) {
        return capitalDAO.addEntry(amount, source, reason);
    }
}

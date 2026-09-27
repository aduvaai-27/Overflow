package com.orderflow.business;

import com.orderflow.dao.SupplierRequestDAO;
import com.orderflow.model.SupplierRequest;

import java.util.List;

public class SupplierRequestService {

    private final SupplierRequestDAO supplierRequestDAO = new SupplierRequestDAO();

    public List<String> phaseFlow() {
        return supplierRequestDAO.phaseFlow();
    }

    public List<SupplierRequest> findAll() {
        return supplierRequestDAO.findAll();
    }

    public boolean createRequest(int supplierId, int productId, int quantity, double unitCost) {
        return supplierRequestDAO.createRequest(supplierId, productId, quantity, unitCost);
    }

    public boolean advancePhase(SupplierRequest request) {
        return supplierRequestDAO.advancePhase(request);
    }
}

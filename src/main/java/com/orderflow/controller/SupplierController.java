package com.orderflow.controller;

import com.orderflow.business.CategoryService;
import com.orderflow.business.ProductService;
import com.orderflow.business.SupplierService;
import com.orderflow.business.SupplierRequestService;
import com.orderflow.model.Category;
import com.orderflow.model.Product;
import com.orderflow.model.Supplier;
import com.orderflow.model.SupplierRequest;
import com.orderflow.util.AlertUtil;
import com.orderflow.util.DateUtil;
import com.orderflow.util.TableColorUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SupplierController extends AbstractCrudController<Supplier> {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private FlowPane categoryCheckboxBox;

    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, Integer> idColumn;
    @FXML private TableColumn<Supplier, String> nameColumn;
    @FXML private TableColumn<Supplier, String> phoneColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, String> addressColumn;
    @FXML private TableColumn<Supplier, String> categoriesColumn;

    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private Label productHintLabel;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private TextField unitCostField;

    @FXML private TableView<SupplierRequest> requestTable;
    @FXML private TableColumn<SupplierRequest, Integer> reqIdColumn;
    @FXML private TableColumn<SupplierRequest, String> reqSupplierColumn;
    @FXML private TableColumn<SupplierRequest, String> reqProductColumn;
    @FXML private TableColumn<SupplierRequest, Integer> reqQtyColumn;
    @FXML private TableColumn<SupplierRequest, Double> reqUnitCostColumn;
    @FXML private TableColumn<SupplierRequest, Double> reqTotalCostColumn;
    @FXML private TableColumn<SupplierRequest, String> reqPhaseColumn;
    @FXML private TableColumn<SupplierRequest, String> reqDateColumn;

    private final SupplierService supplierService = new SupplierService();
    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final SupplierRequestService requestService = new SupplierRequestService();

    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    private final ObservableList<SupplierRequest> requestList = FXCollections.observableArrayList();
    private final List<CheckBox> categoryCheckboxes = new java.util.ArrayList<>();
    private List<Product> allActiveProducts = List.of();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        categoriesColumn.setCellValueFactory(cellData -> {
            String cats = cellData.getValue().getCategoriesDisplay();
            return new javafx.beans.property.SimpleStringProperty(cats == null || cats.isBlank() ? "-" : cats);
        });
        supplierTable.setItems(supplierList);
        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });

        reqIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        reqSupplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        reqProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        reqQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        reqUnitCostColumn.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        reqTotalCostColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        reqPhaseColumn.setCellValueFactory(new PropertyValueFactory<>("phase"));
        TableColorUtil.colorizeText(reqPhaseColumn, value -> switch (value) {
            case "Shipped" -> "#3498db";
            case "Completed" -> "#2ecc71";
            default -> "#e67e22";
        });
        reqDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(DateUtil.formatForDisplay(cellData.getValue().getRequestDate())));
        requestTable.setItems(requestList);

        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 1));

        supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterProductsBySupplier(newVal));

        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                unitCostField.setText(String.format("%.2f", newVal.getPurchasePrice()));
            }
        });

        refreshAll();
    }

    @Override
    protected void refresh() {
        refreshAll();
    }

    private void refreshAll() {
        rebuildCategoryCheckboxes();
        supplierList.setAll(supplierService.findAll());
        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));
        allActiveProducts = productService.findAllActive();
        filterProductsBySupplier(supplierCombo.getValue());
        requestList.setAll(requestService.findAll());
    }

    private void rebuildCategoryCheckboxes() {
        Set<Integer> previouslyChecked = categoryCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .collect(Collectors.toSet());

        categoryCheckboxes.clear();
        categoryCheckboxBox.getChildren().clear();
        for (Category category : categoryService.findAll()) {
            CheckBox checkBox = new CheckBox(category.getName());
            checkBox.setUserData(category.getId());
            checkBox.setSelected(previouslyChecked.contains(category.getId()));
            categoryCheckboxes.add(checkBox);
            categoryCheckboxBox.getChildren().add(checkBox);
        }
    }

    private Set<Integer> selectedCategoryIds() {
        return categoryCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .collect(Collectors.toSet());
    }

    private void filterProductsBySupplier(Supplier supplier) {
        productCombo.getSelectionModel().clearSelection();
        if (supplier == null) {
            productCombo.setItems(FXCollections.observableArrayList(allActiveProducts));
            productHintLabel.setText("Choose a supplier to see only the products they provide.");
            return;
        }
        List<Product> filtered = allActiveProducts.stream()
                .filter(p -> p.getSupplierId() == supplier.getId())
                .collect(Collectors.toList());
        productCombo.setItems(FXCollections.observableArrayList(filtered));
        productHintLabel.setText(filtered.isEmpty()
                ? "No products from " + supplier.getName() + " yet - add them on the Products page."
                : filtered.size() + " product(s) supplied by " + supplier.getName());
    }

    @Override
    protected void populateForm(Supplier s) {
        selectedItem = s;
        nameField.setText(s.getName());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
        addressField.setText(s.getAddress());

        Set<Integer> linkedCategoryIds = supplierService.findCategoryIdsForSupplier(s.getId());
        for (CheckBox cb : categoryCheckboxes) {
            cb.setSelected(linkedCategoryIds.contains((Integer) cb.getUserData()));
        }
    }

    @FXML
    private void handleAddSupplier() {
        Supplier s = buildFromForm(0);
        if (s == null) return;
        if (supplierService.add(s, selectedCategoryIds())) {
            handleClearSupplierForm();
            refreshAll();
        } else {
            AlertUtil.error("Error", "Could not add supplier.");
        }
    }

    @FXML
    private void handleUpdateSupplier() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a supplier from the table first.");
            return;
        }
        Supplier s = buildFromForm(selectedItem.getId());
        if (s == null) return;
        supplierService.update(s, selectedCategoryIds());
        handleClearSupplierForm();
        refreshAll();
    }

    @FXML
    private void handleDeleteSupplier() {
        if (!hasSelection()) {
            AlertUtil.warn("No selection", "Select a supplier from the table first.");
            return;
        }
        if (AlertUtil.confirm("Confirm delete", "Delete supplier '" + selectedItem.getName() + "'?")) {
            if (!supplierService.delete(selectedItem.getId())) {
                AlertUtil.error("Error", "Could not delete this supplier. They still have products assigned or restock requests on file.");
            }
            handleClearSupplierForm();
            refreshAll();
        }
    }

    @FXML
    private void handleClearSupplierForm() {
        clearForm();
    }

    @Override
    protected void clearForm() {
        selectedItem = null;
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        for (CheckBox cb : categoryCheckboxes) cb.setSelected(false);
        supplierTable.getSelectionModel().clearSelection();
    }

    private Supplier buildFromForm(int id) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warn("Validation", "Supplier (company) name is required.");
            return null;
        }
        return new Supplier(id, name, phoneField.getText().trim(), emailField.getText().trim(), addressField.getText().trim());
    }

    @FXML
    private void handleSendRequest() {
        Supplier supplier = supplierCombo.getValue();
        Product product = productCombo.getValue();
        Integer qty = qtySpinner.getValue();

        if (supplier == null) {
            AlertUtil.warn("Validation", "Choose a supplier (company) to request stock from.");
            return;
        }
        if (product == null) {
            AlertUtil.warn("Validation", "Choose a product to restock.");
            return;
        }
        double unitCost;
        try {
            unitCost = Double.parseDouble(unitCostField.getText().trim());
            if (unitCost < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.warn("Validation", "Enter a valid unit cost (what you pay the supplier per unit).");
            return;
        }

        if (requestService.createRequest(supplier.getId(), product.getId(), qty, unitCost)) {
            AlertUtil.info("Request sent", "Restock request sent to " + supplier.getName() + ".");
            refreshAll();
        } else {
            AlertUtil.error("Error", "Could not create the restock request.");
        }
    }

    @FXML
    private void handleAdvancePhase() {
        SupplierRequest request = requestTable.getSelectionModel().getSelectedItem();
        if (request == null) {
            AlertUtil.warn("No selection", "Select a restock request first.");
            return;
        }
        if ("Completed".equals(request.getPhase())) {
            return;
        }

        if (!requestService.advancePhase(request)) {
            AlertUtil.error("Error", "Could not advance this request's phase.");
        }
        refreshAll();
    }

    @FXML
    private void handleRefresh() {
        refreshAll();
    }
}

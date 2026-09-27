package com.orderflow.controller;

import com.orderflow.Main;
import com.orderflow.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {

    @FXML private VBox contentArea;
    @FXML private Label pageTitleLabel;
    @FXML private Label welcomeLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnCapital;
    @FXML private Button btnCategories;
    @FXML private Button btnProducts;
    @FXML private Button btnCustomers;
    @FXML private Button btnOrders;
    @FXML private Button btnSuppliers;
    @FXML private Button btnReports;

    private Object currentSubController;

    @FXML
    public void initialize() {
        if (Session.getCurrentUser() != null) {
            welcomeLabel.setText("Signed in as " + Session.getCurrentUser().getFullName()
                    + " (" + Session.getCurrentUser().getRole() + ")");
        }
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        load("/com/orderflow/fxml/Dashboard.fxml", "Dashboard", btnDashboard);
    }

    @FXML
    public void showCapital() {
        load("/com/orderflow/fxml/Capital.fxml", "Capital", btnCapital);
    }

    @FXML
    public void showCategories() {
        load("/com/orderflow/fxml/Categories.fxml", "Categories", btnCategories);
    }

    @FXML
    public void showProducts() {
        load("/com/orderflow/fxml/Products.fxml", "Products", btnProducts);
    }

    @FXML
    public void showCustomers() {
        load("/com/orderflow/fxml/Customers.fxml", "Customers", btnCustomers);
    }

    @FXML
    public void showOrders() {
        load("/com/orderflow/fxml/Orders.fxml", "Orders", btnOrders);
    }

    @FXML
    public void showSuppliers() {
        load("/com/orderflow/fxml/Suppliers.fxml", "Suppliers", btnSuppliers);
    }

    @FXML
    public void showReports() {
        load("/com/orderflow/fxml/Reports.fxml", "Reports", btnReports);
    }

    @FXML
    public void handleLogout() {
        disposeCurrent();
        Session.logout();
        try {
            Main.switchScene("/com/orderflow/fxml/Login.fxml", "OrderFlow - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(String fxmlPath, String title, Button activeButton) {
        try {
            disposeCurrent();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            currentSubController = loader.getController();

            contentArea.getChildren().setAll(view);
            pageTitleLabel.setText(title);
            highlightActiveButton(activeButton);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disposeCurrent() {
        if (currentSubController instanceof Disposable) {
            ((Disposable) currentSubController).dispose();
        }
    }

    private void highlightActiveButton(Button active) {
        for (Button b : new Button[]{btnDashboard, btnCapital, btnCategories, btnProducts, btnCustomers, btnOrders, btnSuppliers, btnReports}) {
            b.getStyleClass().remove("nav-button-active");
        }
        if (!active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }
}

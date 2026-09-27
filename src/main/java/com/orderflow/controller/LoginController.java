package com.orderflow.controller;

import com.orderflow.Main;
import com.orderflow.business.UserService;
import com.orderflow.model.User;
import com.orderflow.util.PasswordUtil;
import com.orderflow.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        User user = userService.findByUsername(username);

        if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
            showError("Invalid username or password.");
            return;
        }

        Session.login(user);

        try {
            Main.switchScene("/com/orderflow/fxml/MainView.fxml", "OrderFlow - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the main application window.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

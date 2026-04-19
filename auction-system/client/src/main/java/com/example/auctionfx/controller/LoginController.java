package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.model.User;
import com.example.auctionfx.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ApiService apiService = new ApiService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        try {
            User user = apiService.login(username, password);
            if (user != null) {
                AuctionFXApplication.setLoggedInUser(user);
                AuctionFXApplication.showMainView();
            } else {
                errorLabel.setText("Invalid credentials");
            }
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }
}
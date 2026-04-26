package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button showPasswordButton;
    @FXML private Label errorLabel;

    private boolean isPasswordVisible = false;

    private final ApiService apiService = new ApiService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            boolean success = apiService.login(username, password);
            if (success) {
                AuctionFXApplication.setRoot("view/main");
            } else {
                errorLabel.setText("Sai tên đăng nhập hoặc mật khẩu!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            errorLabel.setText("Lỗi: " + msg);
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            showPasswordButton.setText("🔒");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
            showPasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleGoToRegister() throws Exception {
        AuctionFXApplication.setRoot("view/register");
    }
}
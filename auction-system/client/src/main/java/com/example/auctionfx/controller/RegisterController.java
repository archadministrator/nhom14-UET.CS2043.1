package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button showPasswordButton;
    @FXML private Label errorLabel;

    private boolean isPasswordVisible = false;

    private final ApiService apiService = new ApiService();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        try {
            boolean success = apiService.register(username, password, email, "BIDDER");
            
            if (success) {
                AuctionFXApplication.setRoot("view/login");
            } else {
                errorLabel.setText("Đăng ký thất bại. Tên người dùng có thể đã tồn tại.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi kết nối máy chủ!");
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
    private void handleBackToLogin() throws Exception {
        AuctionFXApplication.setRoot("view/login");
    }
}

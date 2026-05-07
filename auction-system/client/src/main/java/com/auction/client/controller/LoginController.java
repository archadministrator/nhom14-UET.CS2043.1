package com.auction.client.controller;

import com.auction.client.model.ClientDto.AuthResponse;
import com.auction.client.service.ApiService;
import com.auction.client.service.SessionManager;
import com.auction.client.util.FxUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField     txtRegUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtRegPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label         lblError;
    @FXML private Button        btnSubmit;
    @FXML private Button        btnTabLogin;
    @FXML private Button        btnTabRegister;
    @FXML private VBox          loginFields;
    @FXML private VBox          registerFields;

    private boolean isLoginMode = true;

    private final ApiService     api     = ApiService.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        cmbRole.getItems().addAll("BIDDER", "SELLER");
        cmbRole.setValue("BIDDER");
    }

    @FXML
    public void switchToLogin() {
        isLoginMode = true;
        loginFields.setVisible(true);
        loginFields.setManaged(true);
        registerFields.setVisible(false);
        registerFields.setManaged(false);
        btnSubmit.setText("Đăng nhập");
        styleActiveTab(btnTabLogin, btnTabRegister);
        hideError();
    }

    @FXML
    public void switchToRegister() {
        isLoginMode = false;
        loginFields.setVisible(false);
        loginFields.setManaged(false);
        registerFields.setVisible(true);
        registerFields.setManaged(true);
        btnSubmit.setText("Đăng ký");
        styleActiveTab(btnTabRegister, btnTabLogin);
        hideError();
    }

    @FXML
    public void handleSubmit() {
        btnSubmit.setDisable(true);
        hideError();

        Thread t = new Thread(() -> {
            try {
                AuthResponse auth = isLoginMode ? doLogin() : doRegister();
                session.login(auth);
                Platform.runLater(() -> {
                    if ("ADMIN".equals(auth.getRole())) {
                        FxUtil.switchScene(btnSubmit, "/fxml/admin-dashboard.fxml", "Quản trị hệ thống");
                    } else {
                        FxUtil.switchScene(btnSubmit, "/fxml/auction-list.fxml", "Danh sách đấu giá");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> btnSubmit.setDisable(false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private AuthResponse doLogin() throws Exception {
        String u = txtUsername.getText().trim();
        String p = txtPassword.getText();
        if (u.isEmpty() || p.isEmpty())
            throw new Exception("Vui lòng nhập đầy đủ thông tin.");
        return api.login(u, p);
    }

    private AuthResponse doRegister() throws Exception {
        String u = txtRegUsername.getText().trim();
        String e = txtEmail.getText().trim();
        String p = txtRegPassword.getText();
        String r = cmbRole.getValue();
        if (u.isEmpty() || e.isEmpty() || p.isEmpty())
            throw new Exception("Vui lòng điền đầy đủ thông tin.");
        return api.register(u, e, p, r);
    }

    private void showError(String msg) {
        lblError.setText(msg != null ? msg : "Lỗi không xác định");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void styleActiveTab(Button active, Button inactive) {
        active.setStyle("-fx-background-color: white; -fx-text-fill: #1565C0; -fx-font-weight: bold;"
                + "-fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 13px;");
        inactive.setStyle("-fx-background-color: transparent; -fx-text-fill: #BBDEFB;"
                + "-fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 13px;");
    }
}
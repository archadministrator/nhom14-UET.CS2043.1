package com.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class FxUtil {

    private FxUtil() {}

    public static void switchScene(Node source, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(FxUtil.class.getResource(fxmlPath));
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root);
            var css = FxUtil.class.getResource("/css/main.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("AuctionSystem — " + title);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Không thể mở màn hình: " + e.getMessage());
        }
    }

    public static void switchSceneWithData(Node source, String fxmlPath, String title,
                                            SceneCallback callback) {
        try {
            FXMLLoader loader = new FXMLLoader(FxUtil.class.getResource(fxmlPath));
            Parent root = loader.load();
            callback.init(loader.getController());
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root);
            var css = FxUtil.class.getResource("/css/main.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("AuctionSystem — " + title);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Không thể mở màn hình: " + e.getMessage());
        }
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Optional<String> showInputDialog(String title, String header, String defaultVal) {
        TextInputDialog dialog = new TextInputDialog(defaultVal);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Nhập giá trị:");
        return dialog.showAndWait();
    }

    @FunctionalInterface
    public interface SceneCallback {
        void init(Object controller);
    }
}
package com.auction.client;

import com.auction.client.service.WebSocketService;
import com.auction.client.util.FxUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

        // Scene duy nhất — dùng suốt vòng đời app, chỉ swap root
        Scene scene = new Scene(root);
        var css = getClass().getResource("/css/main.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setTitle("AuctionSystem");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Đăng ký scene tĩnh để FxUtil dùng lại
        FxUtil.setScene(scene, primaryStage);
    }

    @Override
    public void stop() {
        WebSocketService.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
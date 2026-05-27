package com.auction.client;

import com.auction.client.service.WebSocketService;
import com.auction.client.util.FxUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

        // Lấy kích thước vùng làm việc thực tế (trừ taskbar)
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Scene có kích thước bằng đúng vùng làm việc — không bị cắt
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());
        var css = getClass().getResource("/css/main.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setTitle("AuctionSystem");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Đặt vị trí và kích thước stage khớp vùng làm việc
        primaryStage.setX(screen.getMinX());
        primaryStage.setY(screen.getMinY());
        primaryStage.setWidth(screen.getWidth());
        primaryStage.setHeight(screen.getHeight());

        primaryStage.show();

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
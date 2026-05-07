package com.auction.client;

import com.auction.client.service.WebSocketService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root);

        var css = getClass().getResource("/css/main.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setTitle("AuctionSystem");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(420);
        primaryStage.setMinHeight(520);
        primaryStage.show();
    }

    @Override
    public void stop() {
        WebSocketService.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
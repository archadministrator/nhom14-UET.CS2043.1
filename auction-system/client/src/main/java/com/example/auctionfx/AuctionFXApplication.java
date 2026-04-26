package com.example.auctionfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuctionFXApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        setRoot("view/login");
    }

    public static void setRoot(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionFXApplication.class.getResource(fxml + ".fxml"));
        Parent root = loader.load();
        
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        
        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
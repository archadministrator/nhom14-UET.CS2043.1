package com.example.auctionfx;

import com.example.auctionfx.controller.AuctionDetailController;
import com.example.auctionfx.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuctionFXApplication extends Application {
    private static Stage primaryStage;
    private static User loggedInUser;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLoginView();
    }

    public static void showLoginView() throws Exception {
        Parent root = FXMLLoader.load(AuctionFXApplication.class.getResource("/com/example/auctionfx/view/login.fxml"));
        primaryStage.setTitle("Auction Login");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    public static void showMainView() {
        try {
            Parent root = FXMLLoader.load(AuctionFXApplication.class.getResource("/com/example/auctionfx/view/main.fxml"));
            primaryStage.setTitle("Auction List");
            primaryStage.setScene(new Scene(root, 600, 400));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showAuctionDetail(Long auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(AuctionFXApplication.class.getResource("/com/example/auctionfx/view/auction-detail.fxml"));
            Parent root = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionId(auctionId);
            primaryStage.setTitle("Auction Details");
            primaryStage.setScene(new Scene(root, 500, 400));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
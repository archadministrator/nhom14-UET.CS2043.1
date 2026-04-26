package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.model.Auction;
import com.example.auctionfx.service.ApiService;
import com.example.auctionfx.service.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private FlowPane auctionContainer;
    @FXML private Label userNameLabel;
    @FXML private Button createButton;

    private final ApiService apiService = new ApiService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionManager.getInstance().isLoggedIn()) {
            userNameLabel.setText(SessionManager.getInstance().getCurrentUser().getUsername());
        }
        loadAuctions();
    }

    private void loadAuctions() {
        try {
            List<Auction> auctions = apiService.getAllAuctions();
            Platform.runLater(() -> {
                auctionContainer.getChildren().clear();
                for (Auction auction : auctions) {
                    auctionContainer.getChildren().add(createAuctionCard(auction));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createAuctionCard(Auction auction) {
        VBox card = new VBox(15);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(280);
        card.setAlignment(Pos.TOP_LEFT);

        // Thumbnail Placeholder
        Rectangle thumb = new Rectangle(240, 140);
        thumb.setArcWidth(15);
        thumb.setArcHeight(15);
        thumb.setFill(Color.web("#1e293b"));
        
        Label status = new Label(auction.getStatus().toString());
        status.getStyleClass().addAll("status-badge", "status-running");

        Label title = new Label(auction.getItem().getName());
        title.setStyle("-fx-font-weight: 800; -fx-font-size: 18; -fx-text-fill: white;");
        title.setWrapText(true);

        HBox priceBox = new HBox(5);
        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.setStyle("-fx-text-fill: #94a3b8;");
        Label priceValue = new Label(String.format("%,.0f VND", auction.getCurrentPrice()));
        priceValue.getStyleClass().add("price-tag");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label timer = new Label("⏳ 04:20:15");
        timer.getStyleClass().add("timer-accent");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("Chi tiết");
        viewBtn.setStyle("-fx-padding: 8 15; -fx-font-size: 13;");
        viewBtn.setOnAction(e -> {
            AuctionDetailController.setSelectedAuctionId(auction.getId());
            try {
                AuctionFXApplication.setRoot("view/auction-detail");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        footer.getChildren().addAll(timer, spacer, viewBtn);
        card.getChildren().addAll(thumb, status, title, priceBox, footer);
        
        return card;
    }

    @FXML
    private void handleLogout() throws Exception {
        SessionManager.getInstance().logout();
        AuctionFXApplication.setRoot("view/login");
    }
}
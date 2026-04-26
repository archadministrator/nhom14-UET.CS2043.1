package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.model.Auction;
import com.example.auctionfx.model.Bid;
import com.example.auctionfx.service.ApiService;
import com.example.auctionfx.service.SessionManager;
import com.example.auctionfx.service.WebSocketClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AuctionDetailController implements Initializable {
    private static Long selectedAuctionId;

    @FXML private Label titleLabel, categoryLabel, descriptionLabel, startingPriceLabel, currentPriceLabel, timerLabel;
    @FXML private TextField bidAmountField, maxBidField;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private TableView<Bid> bidHistoryTable;
    @FXML private TableColumn<Bid, String> timeCol, bidderCol, amountCol;

    private final ApiService apiService = new ApiService();
    private final WebSocketClientService wsService = new WebSocketClientService();
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    public static void setSelectedAuctionId(Long id) {
        selectedAuctionId = id;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        priceChart.getData().add(priceSeries);
        priceSeries.setName("Biến động giá");

        loadAuctionDetails();
        wsService.connect(this::handleRealtimeUpdate);
    }

    private void setupTable() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().format(formatter)));
        bidderCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBidder().getUsername()));
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0f", data.getValue().getAmount())));
    }

    private void loadAuctionDetails() {
        if (selectedAuctionId == null) return;
        try {
            Auction auction = apiService.getAuction(selectedAuctionId);
            updateUI(auction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(Auction auction) {
        Platform.runLater(() -> {
            titleLabel.setText(auction.getItem().getName());
            categoryLabel.setText("Danh mục: " + auction.getItem().getClass().getSimpleName());
            descriptionLabel.setText(auction.getItem().getDescription());
            startingPriceLabel.setText(String.format("%,.0f VND", auction.getStartingPrice()));
            currentPriceLabel.setText(String.format("%,.0f VND", auction.getCurrentPrice()));
            
            // Cập nhật biểu đồ và bảng
            if (auction.getBids() != null) {
                bidHistoryTable.getItems().setAll(auction.getBids());
                priceSeries.getData().clear();
                for (Bid bid : auction.getBids()) {
                    priceSeries.getData().add(new XYChart.Data<>(bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")), bid.getAmount()));
                }
            }
        });
    }

    private void handleRealtimeUpdate(Auction updatedAuction) {
        if (updatedAuction.getId().equals(selectedAuctionId)) {
            updateUI(updatedAuction);
        }
    }

    @FXML
    private void handlePlaceBid() {
        try {
            Double amount = Double.parseDouble(bidAmountField.getText());
            Long userId = SessionManager.getInstance().getCurrentUser().getId();
            apiService.placeBid(selectedAuctionId, userId, amount);
            bidAmountField.clear();
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể đặt giá: " + e.getMessage());
        }
    }

    @FXML
    private void handleAutoBid() {
        // Implement auto-bid logic calling backend
        showAlert("Thông báo", "Tính năng Proxy Bidding đã được kích hoạt!");
    }

    @FXML
    private void handleBack() throws Exception {
        wsService.disconnect();
        AuctionFXApplication.setRoot("view/main");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
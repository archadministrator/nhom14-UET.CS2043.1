package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.model.Auction;
import com.example.auctionfx.model.Bid;
import com.example.auctionfx.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.format.DateTimeFormatter;

public class AuctionDetailController {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label endTimeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label messageLabel;

    private ApiService apiService = new ApiService();
    private Long auctionId;

    public void setAuctionId(Long id) {
        this.auctionId = id;
        loadAuctionDetails();
    }

    private void loadAuctionDetails() {
        try {
            Auction auction = apiService.getAuction(auctionId);
            titleLabel.setText(auction.getTitle());
            descriptionLabel.setText(auction.getDescription());
            currentPriceLabel.setText("Current Price: $" + auction.getCurrentPrice());
            endTimeLabel.setText("Ends: " + auction.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } catch (Exception e) {
            messageLabel.setText("Error loading auction details");
        }
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText();
        try {
            Double amount = Double.parseDouble(amountText);
            Long userId = AuctionFXApplication.getLoggedInUser().getId();
            Bid bid = apiService.placeBid(auctionId, userId, amount);
            if (bid != null) {
                messageLabel.setText("Bid placed successfully!");
                loadAuctionDetails(); // refresh current price
            } else {
                messageLabel.setText("Bid failed. Amount must be higher than current price.");
            }
        } catch (NumberFormatException e) {
            messageLabel.setText("Invalid bid amount");
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        AuctionFXApplication.showMainView();
    }
}
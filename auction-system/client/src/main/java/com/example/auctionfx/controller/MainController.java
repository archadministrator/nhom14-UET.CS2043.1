package com.example.auctionfx.controller;

import com.example.auctionfx.AuctionFXApplication;
import com.example.auctionfx.model.Auction;
import com.example.auctionfx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.format.DateTimeFormatter;

public class MainController {
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, Long> idColumn;
    @FXML private TableColumn<Auction, String> titleColumn;
    @FXML private TableColumn<Auction, Double> priceColumn;
    @FXML private TableColumn<Auction, String> endTimeColumn;

    private ApiService apiService = new ApiService();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        // Custom cell factory for date formatting
        endTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEndTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        loadAuctions();
    }

    private void loadAuctions() {
        try {
            auctionTable.setItems(FXCollections.observableArrayList(apiService.getAllAuctions()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadAuctions();
    }

    @FXML
    private void handleViewDetails() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AuctionFXApplication.showAuctionDetail(selected.getId());
        }
    }
}
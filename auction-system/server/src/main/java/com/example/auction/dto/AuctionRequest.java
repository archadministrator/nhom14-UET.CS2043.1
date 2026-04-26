package com.example.auction.dto;

import java.time.LocalDateTime;

public class AuctionRequest {
    private String itemName;
    private String description;
    private String itemType; // "ELECTRONICS", "ART", "VEHICLE"
    private Double startingPrice;
    private LocalDateTime endTime;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}

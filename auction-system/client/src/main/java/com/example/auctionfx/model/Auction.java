package com.example.auctionfx.model;

import java.time.LocalDateTime;

public class Auction {
    private Long id;
    private String title;
    private String description;
    private Double startingPrice;
    private Double currentPrice;
    private LocalDateTime endTime;
    private User seller;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
}
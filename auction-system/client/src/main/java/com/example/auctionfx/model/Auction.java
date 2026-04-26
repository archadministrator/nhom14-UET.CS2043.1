package com.example.auctionfx.model;

import java.time.LocalDateTime;
import java.util.List;

public class Auction {
    private Long id;
    private Item item;
    private Double startingPrice;
    private Double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private User seller;
    private User winner;
    private List<Bid> bids;

    public Auction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> bids) { this.bids = bids; }
}
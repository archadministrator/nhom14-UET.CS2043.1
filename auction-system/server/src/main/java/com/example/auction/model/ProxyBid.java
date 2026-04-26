package com.example.auction.model;

import jakarta.persistence.*;

/**
 * Thực thể lưu trữ mức giá đặt thầu tự động tối đa (Proxy Bidding).
 */
@Entity
@Table(name = "proxy_bids")
public class ProxyBid extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne
    @JoinColumn(name = "bidder_id")
    private User bidder;

    private Double maxAmount;

    public ProxyBid() {}

    // Getters and Setters
    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }
    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
}

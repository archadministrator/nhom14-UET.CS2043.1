package com.example.auction.dto;

public class ProxyBidRequest {
    private Long auctionId;
    private Double maxAmount;

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
}

package com.example.auction.service.strategy;

import com.example.auction.model.Auction;
import com.example.auction.model.Bid;
import org.springframework.stereotype.Component;

/**
 * Triển khai cụ thể cho Đấu giá Anh.
 * Kế thừa BiddingStrategy và triển khai logic so sánh giá (isBidValid).
 */
@Component
public class EnglishBiddingStrategy extends BiddingStrategy {

    @Override
    protected boolean isBidValid(Auction auction, Bid newBid) {
        Double currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartingPrice();
        return newBid.getAmount() > currentPrice;
    }
}

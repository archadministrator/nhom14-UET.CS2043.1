package com.example.auction.service.strategy;

import com.example.auction.model.Auction;
import com.example.auction.model.Bid;
import org.springframework.stereotype.Component;

/**
 * Triển khai Đấu giá Hà Lan (Dutch Auction).
 * Chiến lược: Giá bắt đầu từ mức cao và giảm dần. 
 * Người đầu tiên đặt giá bằng hoặc cao hơn giá hiện tại sẽ thắng cuộc ngay lập tức.
 */
@Component
public class DutchBiddingStrategy extends BiddingStrategy {

    @Override
    protected boolean isBidValid(Auction auction, Bid newBid) {
        // Trong Đấu giá Hà Lan, người mua chấp nhận giá hiện tại là thắng luôn
        return newBid.getAmount() >= auction.getCurrentPrice();
    }

    @Override
    protected void updateAuctionPrice(Auction auction, Bid newBid) {
        super.updateAuctionPrice(auction, newBid);
        // Ngay khi có người đặt giá thành công, kết thúc phiên đấu giá (Thắng cuộc ngay)
        auction.setStatus(com.example.auction.model.AuctionStatus.FINISHED);
    }
}

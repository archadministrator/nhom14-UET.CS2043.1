package com.example.auction.service.observer;

import com.example.auction.model.Auction;

public interface AuctionObserver {
    /**
     * Phương thức được gọi khi có sự kiện thay đổi trong cuộc đấu giá.
     * @param event Loại sự kiện
     * @param auction Đối tượng cuộc đấu giá liên quan
     */
    void onAuctionEvent(AuctionEvent event, Auction auction);
}

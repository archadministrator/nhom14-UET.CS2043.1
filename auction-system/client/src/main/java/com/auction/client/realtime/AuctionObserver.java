package com.auction.client.realtime;

@FunctionalInterface
public interface AuctionObserver {
    void onEvent(AuctionEvent event);
}
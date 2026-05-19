package com.auction.client.realtime;

/**
 * Observer interface cho bất kỳ component nào muốn nhận realtime event.
 * Implement bởi: AuctionDetailController, AuctionListController.
 *
 * onEvent() LUÔN được gọi trên JavaFX Application Thread
 * (đảm bảo bởi AuctionEventBus) → controller không cần Platform.runLater().
 */
@FunctionalInterface
public interface AuctionObserver {
    void onEvent(AuctionEvent event);
}
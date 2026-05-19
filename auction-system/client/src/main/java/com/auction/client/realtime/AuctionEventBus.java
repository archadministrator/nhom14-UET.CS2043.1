package com.auction.client.realtime;

import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe Event Bus cho realtime auction events.
 *
 * Thiết kế:
 *  - ConcurrentHashMap<auctionId, observers>: nhiều thread đọc/ghi an toàn
 *  - CopyOnWriteArrayList: iteration không cần lock, phù hợp read-heavy
 *  - Dispatch đảm bảo chạy trên JavaFX Application Thread
 *  - Global observers nhận TẤT CẢ event (dùng cho AuctionListController)
 *  - Per-auction observers chỉ nhận event của phiên đó (dùng cho AuctionDetailController)
 *
 * Singleton — sống cùng vòng đời ứng dụng.
 */
public final class AuctionEventBus {

    private static volatile AuctionEventBus instance;

    /** auctionId → danh sách observers của phiên đó */
    private final Map<Long, CopyOnWriteArrayList<AuctionObserver>> perAuctionObservers
            = new ConcurrentHashMap<>();

    /** Observers nhận mọi event, không phân biệt auctionId */
    private final CopyOnWriteArrayList<AuctionObserver> globalObservers
            = new CopyOnWriteArrayList<>();

    private AuctionEventBus() {}

    public static AuctionEventBus getInstance() {
        if (instance == null) {
            synchronized (AuctionEventBus.class) {
                if (instance == null) instance = new AuctionEventBus();
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────
    // Subscribe / Unsubscribe
    // ─────────────────────────────────────────────────────────────────

    /**
     * Đăng ký observer cho một phiên cụ thể.
     * An toàn để gọi từ bất kỳ thread nào.
     */
    public void subscribe(long auctionId, AuctionObserver observer) {
        perAuctionObservers
                .computeIfAbsent(auctionId, id -> new CopyOnWriteArrayList<>())
                .add(observer);
    }

    /**
     * Hủy đăng ký observer của một phiên.
     * Nên gọi khi controller bị destroy (rời màn hình).
     */
    public void unsubscribe(long auctionId, AuctionObserver observer) {
        CopyOnWriteArrayList<AuctionObserver> list = perAuctionObservers.get(auctionId);
        if (list != null) {
            list.remove(observer);
            if (list.isEmpty()) perAuctionObservers.remove(auctionId);
        }
    }

    /** Hủy toàn bộ observers của một phiên (dùng khi navigate away). */
    public void unsubscribeAll(long auctionId) {
        perAuctionObservers.remove(auctionId);
    }

    /** Đăng ký observer nhận TẤT CẢ events (không phân biệt phiên). */
    public void subscribeGlobal(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    /** Hủy global observer. */
    public void unsubscribeGlobal(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    // ─────────────────────────────────────────────────────────────────
    // Publish
    // ─────────────────────────────────────────────────────────────────

    /**
     * Publish event đến tất cả observers liên quan.
     * An toàn để gọi từ bất kỳ thread nào (WebSocket thread, background thread...).
     * Dispatch đảm bảo chạy trên JavaFX Application Thread.
     */
    public void publish(AuctionEvent event) {
        // Gom danh sách observers cần notify
        CopyOnWriteArrayList<AuctionObserver> specific =
                perAuctionObservers.get(event.getAuctionId());

        List<AuctionObserver> toNotify = new java.util.ArrayList<>();
        if (specific != null) toNotify.addAll(specific);
        toNotify.addAll(globalObservers);

        if (toNotify.isEmpty()) return;

        // Đảm bảo luôn chạy trên FX thread — observers không cần Platform.runLater()
        if (Platform.isFxApplicationThread()) {
            dispatchAll(toNotify, event);
        } else {
            Platform.runLater(() -> dispatchAll(toNotify, event));
        }
    }

    private void dispatchAll(List<AuctionObserver> observers, AuctionEvent event) {
        for (AuctionObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                System.err.println("[EventBus] Observer error for auctionId="
                        + event.getAuctionId() + ": " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Debug
    // ─────────────────────────────────────────────────────────────────

    public int observerCount(long auctionId) {
        CopyOnWriteArrayList<AuctionObserver> list = perAuctionObservers.get(auctionId);
        return list == null ? 0 : list.size();
    }
}
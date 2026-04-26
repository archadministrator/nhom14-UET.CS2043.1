package com.example.auction.service.observer;

import com.example.auction.model.Auction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Lớp đóng vai trò "Subject" trong Observer Pattern.
 * Quản lý danh sách các Observer và thông báo cho họ khi có sự kiện.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final List<AuctionObserver> observers = new ArrayList<>();
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Đăng ký một người quan sát mới.
     */
    public void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    /**
     * Thông báo cho tất cả người quan sát về một sự kiện (Bất đồng bộ - Đa luồng).
     */
    @Async
    public void notifyObservers(AuctionEvent event, Auction auction) {
        logger.info("Đang xử lý thông báo cho sự kiện: {} của Auction ID: {}", event, auction.getId());
        
        // Broadcast qua WebSocket (Real-time) - Giải quyết vấn đề số 2
        messagingTemplate.convertAndSend("/topic/auctions", auction);

        for (AuctionObserver observer : observers) {
            try {
                observer.onAuctionEvent(event, auction);
            } catch (Exception e) {
                logger.error("Lỗi khi gửi thông báo tới một observer: {}", e.getMessage());
            }
        }
    }
}

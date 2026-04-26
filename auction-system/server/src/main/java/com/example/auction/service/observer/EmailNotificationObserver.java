package com.example.auction.service.observer;

import com.example.auction.model.Auction;
import com.example.auction.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Một trình quan sát cụ thể giả lập việc gửi email thông báo.
 */
@Component
public class EmailNotificationObserver implements AuctionObserver {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationObserver.class);
    private final NotificationService notificationService;

    public EmailNotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        notificationService.registerObserver(this);
    }

    @Override
    public void onAuctionEvent(AuctionEvent event, Auction auction) {
        User seller = auction.getSeller();
        String sellerEmail = seller != null ? seller.getUsername() + "@example.com" : "unknown@example.com";

        switch (event) {
            case NEW_BID:
                logger.info("[EMAIL] Gửi tới {}: Có người vừa đặt giá mới cho sản phẩm '{}'. Giá hiện tại: {}", 
                        sellerEmail, auction.getItem().getName(), auction.getCurrentPrice());
                break;
            case AUCTION_ENDED:
                logger.info("[EMAIL] Gửi tới {}: Cuộc đấu giá '{}' đã kết thúc thành công!", 
                        sellerEmail, auction.getItem().getName());
                break;
            case BID_REJECTED:
                logger.info("[EMAIL] Thông báo: Một giá thầu cho '{}' đã bị từ chối.", auction.getItem().getName());
                break;
        }
    }
}

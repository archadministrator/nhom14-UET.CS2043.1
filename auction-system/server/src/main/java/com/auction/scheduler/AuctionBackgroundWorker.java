package com.auction.scheduler;

import com.auction.model.AuctionItem;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionBackgroundWorker {

    private final AuctionService auctionService;
    private final BidService bidService;

    /**
     * Mỗi 3 giây: tìm các phiên OPEN đã đến giờ bắt đầu → chuyển sang RUNNING
     * và broadcast AUCTION_STARTED đến tất cả client.
     */
    @Scheduled(fixedDelay = 3_000)
    public void activateAuctions() {
        List<AuctionItem> toActivate = auctionService.findItemsToActivate();
        if (toActivate.isEmpty()) return;

        log.info("[Scheduler] Kích hoạt {} phiên đấu giá", toActivate.size());
        for (AuctionItem item : toActivate) {
            try {
                auctionService.activateAuction(item);
                bidService.broadcastAuctionStarted(item);
                log.info("[Scheduler] Phiên [{}] '{}' → RUNNING", item.getId(), item.getName());
            } catch (Exception e) {
                log.error("[Scheduler] Lỗi khi kích hoạt phiên [{}]: {}", item.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Mỗi 3 giây: tìm các phiên RUNNING đã hết giờ → đóng phiên,
     * xác định winner (nếu có bid), broadcast AUCTION_CLOSED đến tất cả client.
     */
    @Scheduled(fixedDelay = 3_000)
    public void closeAuctions() {
        List<AuctionItem> toClose = auctionService.findItemsToClose();
        if (toClose.isEmpty()) return;

        log.info("[Scheduler] Đóng {} phiên đấu giá", toClose.size());
        for (AuctionItem item : toClose) {
            try {
                auctionService.closeAuction(item);
                bidService.broadcastAuctionClosed(item);
                log.info("[Scheduler] Phiên [{}] '{}' → {} (winner: {})",
                        item.getId(), item.getName(),
                        item.getStatus(),
                        item.getWinner() != null ? item.getWinner().getUsername() : "không có");
            } catch (Exception e) {
                log.error("[Scheduler] Lỗi khi đóng phiên [{}]: {}", item.getId(), e.getMessage(), e);
            }
        }
    }
}
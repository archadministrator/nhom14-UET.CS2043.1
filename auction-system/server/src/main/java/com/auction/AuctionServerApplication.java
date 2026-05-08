package com.auction.scheduler;

import com.auction.model.AuctionItem;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background Worker thủ công dùng Thread để minh họa Concurrency.
 * - Tạo một Daemon Thread riêng biệt để quét và xử lý phiên đấu giá.
 * - Kích hoạt phiên (OPEN -> RUNNING) khi đến giờ bắt đầu.
 * - Đóng phiên (RUNNING -> FINISHED/CANCELED) khi hết giờ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionServerApplication {

    private final AuctionService auctionService;
    private final BidService bidService;

    @PostConstruct
    public void startWorker() {
        Thread workerThread = new Thread(() -> {
            log.info("Auction Background Worker đã bắt đầu chạy...");
            while (true) {
                try {
                    // 1. Quét kích hoạt các phiên sắp đến giờ
                    activateAuctions();

                    // 2. Quét đóng các phiên đã hết giờ
                    closeAuctions();

                    // Nghỉ 10 giây trước vòng quét tiếp theo
                    Thread.sleep(10000);

                } catch (InterruptedException e) {
                    log.warn("Background Worker bị gián đoạn, dừng lại.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Lỗi không xác định trong Background Worker: {}", e.getMessage());
                    // Không break -> tiếp tục chạy dù có lỗi
                }
            }
        });
        workerThread.setName("auction-background-worker");
        workerThread.setDaemon(true); // Tự động dừng khi JVM tắt
        workerThread.start();
    }

    private void activateAuctions() {
        List<AuctionItem> items = auctionService.findItemsToActivate();
        for (AuctionItem item : items) {
            try {
                auctionService.activateAuction(item);
                bidService.broadcastAuctionStarted(item);
            } catch (Exception e) {
                log.error("Lỗi khi kích hoạt phiên [{}]: {}", item.getId(), e.getMessage());
            }
        }
    }

    private void closeAuctions() {
        List<AuctionItem> items = auctionService.findItemsToClose();
        for (AuctionItem item : items) {
            try {
                auctionService.closeAuction(item);
                bidService.broadcastAuctionClosed(item);
            } catch (Exception e) {
                log.error("Lỗi khi đóng phiên [{}]: {}", item.getId(), e.getMessage());
            }
        }
    }
}


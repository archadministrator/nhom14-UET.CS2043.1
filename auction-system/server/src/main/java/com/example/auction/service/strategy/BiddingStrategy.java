package com.example.auction.service.strategy;

import com.example.auction.model.Auction;
import com.example.auction.model.AuctionStatus;
import com.example.auction.model.Bid;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng định nghĩa khung quy trình đặt giá.
 * Sử dụng mẫu thiết kế Template Method để đảm bảo tính nhất quán giữa các loại đấu giá.
 */
public abstract class BiddingStrategy {

    /**
     * Quy trình Template Method để xử lý một giá thầu.
     * Các bước chung (kiểm tra trạng thái, thời gian) được cố định ở đây.
     * Logic đặc thù (so sánh giá) được ủy quyền cho các lớp con.
     */
    public final boolean processBid(Auction auction, Bid newBid) {
        // 1. Kiểm tra trạng thái chung (Bước cố định)
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return false;
        }

        // 2. Kiểm tra thời gian hết hạn (Bước cố định)
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            return false;
        }

        // 3. Kiểm tra số dư tài khoản (Wallet check)
        if (newBid.getBidder().getBalance() < newBid.getAmount()) {
            return false;
        }

        // 4. Thực hiện logic đặt giá đặc thù (Bước trừu tượng - Đa hình)
        if (!isBidValid(auction, newBid)) {
            return false;
        }

        // 4. Cập nhật giá sản phẩm (Bước cố định)
        updateAuctionPrice(auction, newBid);
        return true;
    }

    /**
     * Logic kiểm tra giá thầu cụ thể. Phải được triển khai bởi các lớp con.
     */
    protected abstract boolean isBidValid(Auction auction, Bid newBid);

    /**
     * Phương thức mặc định để cập nhật giá. Có thể ghi đè nếu cần (**Hook**).
     */
    protected void updateAuctionPrice(Auction auction, Bid newBid) {
        auction.setCurrentPrice(newBid.getAmount());
    }
}

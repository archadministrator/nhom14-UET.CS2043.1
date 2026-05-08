package com.auction.model.enums;

public enum AuctionStatus {
    OPEN,       // Tạo xong, chờ đến giờ bắt đầu
    RUNNING,    // Đang diễn ra, nhận bid
    FINISHED,   // Hết giờ, đã xác định winner
    PAID,       // Winner đã thanh toán
    CANCELED    // Không có ai đặt giá hoặc bị hủy
}

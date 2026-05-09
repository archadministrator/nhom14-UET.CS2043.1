package com.auction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException() {
        super("Phiên đấu giá đã đóng hoặc chưa bắt đầu.");
    }
}
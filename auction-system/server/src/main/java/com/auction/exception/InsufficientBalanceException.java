package com.auction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Số dư không đủ để đặt giá.");
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
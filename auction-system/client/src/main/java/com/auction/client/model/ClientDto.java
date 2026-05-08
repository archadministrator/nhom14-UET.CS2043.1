package com.auction.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientDto {

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String role;
        private BigDecimal balance;
        private boolean active;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private String role;
        private BigDecimal balance;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuctionDto {
        private Long id;
        private UserDto seller;
        private String name;
        private String description;
        private BigDecimal startPrice;
        private BigDecimal currentPrice;
        private BigDecimal minIncrement;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private UserDto winner;
        private String imageUrl;
        private long totalBids;
        private LocalDateTime createdAt;

        public boolean isRunning()  { return "RUNNING".equals(status); }
        public boolean isOpen()     { return "OPEN".equals(status); }
        public boolean isFinished() {
            return "FINISHED".equals(status) || "PAID".equals(status) || "CANCELED".equals(status);
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidDto {
        private Long id;
        private Long auctionId;
        private UserDto bidder;
        private BigDecimal amount;
        private boolean autoBid;
        private LocalDateTime bidTime;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidUpdateMessage {
        private String type;
        private Long auctionId;
        private BigDecimal currentPrice;
        private String leaderUsername;
        private long totalBids;
        private LocalDateTime endTime;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorResponse {
        private int status;
        private String message;
    }
}
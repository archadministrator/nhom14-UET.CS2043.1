package com.auction.util;

import com.auction.model.AuctionItem;
import com.auction.model.Bid;
import com.auction.model.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dto {

    // ── AUTH ─────────────────────────────────────────────────────────

    public record RegisterRequest(
            @NotBlank(message = "Username không được để trống")
            @Size(min = 3, max = 50, message = "Username 3-50 ký tự")
            String username,

            @NotBlank
            @Email(message = "Email không hợp lệ")
            String email,

            @NotBlank
            @Size(min = 6, message = "Mật khẩu ít nhất 6 ký tự")
            String password,

            @NotNull
            Role role
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            String username,
            String email,
            Role role,
            BigDecimal balance
    ) {}

    // ── USER ─────────────────────────────────────────────────────────

    public record UserResponse(
            Long id,
            String username,
            String email,
            Role role,
            BigDecimal balance,
            boolean active,
            LocalDateTime createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getRole(),
                    u.getBalance(),
                    u.isActive(),
                    u.getCreatedAt()
            );
        }
    }

    // ── AUCTION ───────────────────────────────────────────────────────

    public record CreateAuctionRequest(
            @NotBlank(message = "Tên sản phẩm không được để trống")
            @Size(max = 200)
            String name,

            String description,

            @NotNull
            @DecimalMin("0.01")
            BigDecimal startPrice,

            @NotNull
            @DecimalMin("1000")
            BigDecimal minIncrement,

            @NotNull
            @Future(message = "Thời gian bắt đầu phải trong tương lai")
            LocalDateTime startTime,

            @NotNull
            @Future(message = "Thời gian kết thúc phải trong tương lai")
            LocalDateTime endTime,

            String imageUrl
    ) {}

    public record UpdateAuctionRequest(
            @Size(max = 200) String name,
            String description,
            String imageUrl
    ) {}

    public record AuctionResponse(
            Long id,
            UserResponse seller,
            String name,
            String description,
            BigDecimal startPrice,
            BigDecimal currentPrice,
            BigDecimal minIncrement,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            UserResponse winner,
            String imageUrl,
            long totalBids,
            LocalDateTime createdAt
    ) {
        public static AuctionResponse from(AuctionItem a, long bidCount) {
            return new AuctionResponse(
                    a.getId(),
                    UserResponse.from(a.getSeller()),
                    a.getName(),
                    a.getDescription(),
                    a.getStartPrice(),
                    a.getCurrentPrice(),
                    a.getMinIncrement(),
                    a.getStartTime(),
                    a.getEndTime(),
                    a.getStatus(),
                    a.getWinner() != null ? UserResponse.from(a.getWinner()) : null,
                    a.getImageUrl(),
                    bidCount,
                    a.getCreatedAt()
            );
        }
    }

    // ── BID ───────────────────────────────────────────────────────────

    public record PlaceBidRequest(
            @NotNull @DecimalMin("1") BigDecimal amount
    ) {}

    @Builder
    public record BidResponse(
            Long id,
            Long auctionId,
            String auctionName,
            UserResponse bidder,
            BigDecimal amount,
            boolean isAutoBid,
            LocalDateTime bidTime
    ) {
        public static BidResponse from(Bid b) {
            return BidResponse.builder()
                    .id(b.getId())
                    .auctionId(b.getAuctionItem().getId())
                    .auctionName(b.getAuctionItem().getName())
                    .bidder(UserResponse.from(b.getBidder()))
                    .amount(b.getAmount())
                    .isAutoBid(b.isAutoBid())
                    .bidTime(b.getBidTime())
                    .build();
        }
    }

    // ── AUTO-BID ──────────────────────────────────────────────────────

    public record AutoBidRequest(
            @NotNull @DecimalMin("1") BigDecimal maxAmount,
            @NotNull @DecimalMin("1000") BigDecimal increment
    ) {}

    // ── TOPUP ─────────────────────────────────────────────────────────

    public record TopUpRequest(
            @NotNull @DecimalMin("1000") BigDecimal amount
    ) {}

    // ── WEBSOCKET MESSAGE ─────────────────────────────────────────────

    public record BidUpdateMessage(
            String type,
            Long auctionId,
            BigDecimal currentPrice,
            String leaderUsername,
            long totalBids,
            LocalDateTime endTime
    ) {}

    public record AccountStatusMessage(
            String type,
            String username,
            String message
    ) {}
}
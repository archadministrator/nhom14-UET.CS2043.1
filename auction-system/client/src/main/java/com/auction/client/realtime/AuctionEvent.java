package com.auction.client.realtime;

import com.auction.client.model.ClientDto.BidUpdateMessage;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Typed event được publish qua AuctionEventBus.
 * Wrap BidUpdateMessage thô thành domain event có kiểu rõ ràng.
 */
@Getter
public final class AuctionEvent {

    public enum Type {
        NEW_BID,
        AUCTION_CREATED,
        AUCTION_STARTED,
        AUCTION_CLOSED,
        AUCTION_CANCELED
    }

    private final Type        type;
    private final long        auctionId;
    private final BigDecimal  currentPrice;
    private final String      leaderUsername;
    private final long        totalBids;
    private final LocalDateTime endTime;

    private AuctionEvent(Type type, long auctionId, BigDecimal currentPrice,
                         String leaderUsername, long totalBids, LocalDateTime endTime) {
        this.type           = type;
        this.auctionId      = auctionId;
        this.currentPrice   = currentPrice;
        this.leaderUsername = leaderUsername;
        this.totalBids      = totalBids;
        this.endTime        = endTime;
    }

    /** Factory: chuyển BidUpdateMessage thô → AuctionEvent có type. */
    public static AuctionEvent from(BidUpdateMessage msg) {
        Type t = switch (msg.getType()) {
            case "AUCTION_CREATED"  -> Type.AUCTION_CREATED;
            case "AUCTION_STARTED"  -> Type.AUCTION_STARTED;
            case "AUCTION_CLOSED"   -> Type.AUCTION_CLOSED;
            case "AUCTION_CANCELED" -> Type.AUCTION_CANCELED;
            default                 -> Type.NEW_BID;
        };
        return new AuctionEvent(t, msg.getAuctionId(), msg.getCurrentPrice(),
                msg.getLeaderUsername(), msg.getTotalBids(), msg.getEndTime());
    }
}
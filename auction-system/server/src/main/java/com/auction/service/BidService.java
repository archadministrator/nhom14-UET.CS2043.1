package com.auction.service;

import com.auction.dao.BidRepository;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionItem;
import com.auction.model.Bid;
import com.auction.model.User;
import com.auction.util.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class BidService {

    private final BidRepository bidRepo;
    private final AuctionService auctionService;
    private final UserService userService;
    private final AutoBidService autoBidService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public BidService(BidRepository bidRepo,
                      AuctionService auctionService,
                      UserService userService,
                      @Lazy AutoBidService autoBidService,
                      SimpMessagingTemplate messagingTemplate) {
        this.bidRepo = bidRepo;
        this.auctionService = auctionService;
        this.userService = userService;
        this.autoBidService = autoBidService;
        this.messagingTemplate = messagingTemplate;
    }

    private ReentrantLock getLockForAuction(Long auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    @Transactional
    public synchronized Dto.BidResponse placeBid(Long auctionId, Dto.PlaceBidRequest req, String bidderUsername) {
        return doPlaceBid(auctionId, req.amount(), bidderUsername, false);
    }

    Dto.BidResponse doPlaceBid(Long auctionId, BigDecimal amount,
                               String bidderUsername, boolean isAuto) {
        AuctionItem item = auctionService.findById(auctionId);
        User bidder = userService.findByUsername(bidderUsername);

        if (!item.isAcceptingBids())
            throw new AuctionClosedException();

        if (item.getSeller().getUsername().equals(bidderUsername))
            throw new InvalidBidException("Người bán không thể tự đặt giá sản phẩm của mình.");

        BigDecimal minBid = item.minimumNextBid();
        if (amount.compareTo(minBid) < 0)
            throw new InvalidBidException(String.format(
                    "Giá đặt tối thiểu là %,.0f₫ (hiện tại %,.0f₫ + mức tăng %,.0f₫)",
                    minBid, item.getCurrentPrice(), item.getMinIncrement()));

        // 1. Lưu thông tin người đang giữ giá cao nhất để hoàn tiền
        Optional<Bid> topBidOpt = bidRepo.findTopBidByAuction(item);

        // 2. Thử trừ tiền người đặt giá mới (Nếu không đủ tiền sẽ văng ngoại lệ và Transaction sẽ rollback)
        userService.subtractBalance(bidderUsername, amount);

        // 3. Nếu trừ tiền thành công, tiến hành cập nhật đấu giá
        // Anti-sniping: bid trong 5 phút cuối → gia hạn thêm 5 phút
        LocalDateTime now = LocalDateTime.now();
        if (item.getEndTime().minusMinutes(5).isBefore(now)) {
            item.setEndTime(item.getEndTime().plusMinutes(5));
            log.info("Auction [{}] gia hạn anti-sniping → endTime: {}", auctionId, item.getEndTime());
        }

        item.setCurrentPrice(amount);

        Bid bid = Bid.builder()
                .auctionItem(item)
                .bidder(bidder)
                .amount(amount)
                .isAutoBid(isAuto)
                .build();

        Bid savedBid = bidRepo.save(bid);

        // 4. Hoàn tiền cho người bị vượt mặt (người giữ giá cũ)
        if (topBidOpt.isPresent()) {
            Bid oldTopBid = topBidOpt.get();
            userService.addBalance(oldTopBid.getBidder().getUsername(), oldTopBid.getAmount());
            log.info("Hoàn tiền cho người giữ giá cũ: {} số tiền {}",
                    oldTopBid.getBidder().getUsername(), oldTopBid.getAmount());
        }

        log.info("Bid [{}] auction={} bidder={} amount={} auto={}",
                savedBid.getId(), auctionId, bidderUsername, amount, isAuto);

        broadcastBidUpdate(item);
        autoBidService.triggerAutoBids(item, bidder);

        return Dto.BidResponse.from(savedBid);
    }

    @Transactional(readOnly = true)
    public List<Dto.BidResponse> getBidHistory(Long auctionId) {
        AuctionItem item = auctionService.findById(auctionId);
        return bidRepo.findBidHistoryByAuction(item.getId())
                .stream().map(Dto.BidResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.BidResponse> getMyBids(String username) {
        User user = userService.findByUsername(username);
        return bidRepo.findByBidderOrderByBidTimeDesc(user)
                .stream().map(Dto.BidResponse::from).toList();
    }

    void broadcastBidUpdate(AuctionItem item) {
        long totalBids = bidRepo.countByAuctionItem(item);
        String leader = bidRepo.findTopBidByAuction(item)
                .map(b -> b.getBidder().getUsername())
                .orElse(null);

        Dto.BidUpdateMessage msg = new Dto.BidUpdateMessage(
                "NEW_BID", item.getId(), item.getCurrentPrice(),
                leader, totalBids, item.getEndTime());

        messagingTemplate.convertAndSend("/topic/auction/" + item.getId(), msg);
    }

    public void broadcastAuctionClosed(AuctionItem item) {
        long totalBids = bidRepo.countByAuctionItem(item);
        String winner = item.getWinner() != null ? item.getWinner().getUsername() : null;

        Dto.BidUpdateMessage msg = new Dto.BidUpdateMessage(
                "AUCTION_CLOSED", item.getId(), item.getCurrentPrice(),
                winner, totalBids, item.getEndTime());

        messagingTemplate.convertAndSend("/topic/auction/" + item.getId(), msg);
    }

    public void broadcastAuctionStarted(AuctionItem item) {
        Dto.BidUpdateMessage msg = new Dto.BidUpdateMessage(
                "AUCTION_STARTED", item.getId(), item.getCurrentPrice(),
                null, 0L, item.getEndTime());

        messagingTemplate.convertAndSend("/topic/auctions", msg);
    }

    public ReentrantLock getLock(Long auctionId) {
        return getLockForAuction(auctionId);
    }
}
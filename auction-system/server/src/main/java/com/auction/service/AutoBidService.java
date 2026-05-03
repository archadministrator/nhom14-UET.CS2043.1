package com.auction.service;

import com.auction.dao.AutoBidConfigRepository;
import com.auction.model.AuctionItem;
import com.auction.model.AutoBidConfig;
import com.auction.model.User;
import com.auction.util.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class AutoBidService {

    private final AutoBidConfigRepository configRepo;
    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public AutoBidService(AutoBidConfigRepository configRepo,
                          UserService userService,
                          AuctionService auctionService,
                          @Lazy BidService bidService) {
        this.configRepo = configRepo;
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    @Transactional
    public AutoBidConfig setupAutoBid(Long auctionId, Dto.AutoBidRequest req, String username) {
        User bidder = userService.findByUsername(username);
        AuctionItem item = auctionService.findById(auctionId);

        if (!item.isAcceptingBids())
            throw new IllegalStateException("Phiên đấu giá không đang chạy.");

        if (req.maxAmount().compareTo(item.getCurrentPrice()) <= 0)
            throw new IllegalArgumentException("Giá tối đa phải lớn hơn giá hiện tại.");

        AutoBidConfig config = configRepo
                .findByBidderAndAuctionItem(bidder, item)
                .orElse(AutoBidConfig.builder().bidder(bidder).auctionItem(item).build());

        config.setMaxAmount(req.maxAmount());
        config.setIncrement(req.increment());
        config.setActive(true);

        AutoBidConfig saved = configRepo.save(config);
        log.info("AutoBid setup: bidder={} auction={} maxAmount={}",
                username, auctionId, req.maxAmount());

        triggerAutoBids(item, null);
        return saved;
    }

    @Transactional
    public void cancelAutoBid(Long auctionId, String username) {
        User bidder = userService.findByUsername(username);
        AuctionItem item = auctionService.findById(auctionId);

        configRepo.findByBidderAndAuctionItem(bidder, item).ifPresent(cfg -> {
            cfg.setActive(false);
            configRepo.save(cfg);
            log.info("AutoBid cancelled: bidder={} auction={}", username, auctionId);
        });
    }

    @Transactional
    public void triggerAutoBids(AuctionItem item, User currentLeader) {
        if (!item.isAcceptingBids()) return;

        User leader = currentLeader;
        if (leader == null) {
            List<Dto.BidResponse> history = bidService.getBidHistory(item.getId());
            if (!history.isEmpty()) {
                leader = userService.findByUsername(history.get(0).bidder().username());
            }
        }

        List<AutoBidConfig> configs = configRepo.findActiveConfigsExcluding(item, leader);
        if (configs.isEmpty()) return;

        AutoBidConfig topConfig = configs.get(0);
        BigDecimal nextBid = item.getCurrentPrice().add(topConfig.getIncrement());

        if (nextBid.compareTo(topConfig.getMaxAmount()) > 0) {
            topConfig.setActive(false);
            configRepo.save(topConfig);
            log.info("AutoBid maxAmount reached: bidder={} auction={}",
                    topConfig.getBidder().getUsername(), item.getId());
            return;
        }

        ReentrantLock lock = bidService.getLock(item.getId());
        lock.lock();
        try {
            AuctionItem freshItem = auctionService.findById(item.getId());
            BigDecimal freshNext = freshItem.getCurrentPrice().add(topConfig.getIncrement());

            if (freshNext.compareTo(topConfig.getMaxAmount()) <= 0) {
                bidService.doPlaceBid(item.getId(), freshNext,
                        topConfig.getBidder().getUsername(), true);
            }
        } finally {
            lock.unlock();
        }
    }
}
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
import java.util.PriorityQueue;
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

    public void triggerAutoBids(AuctionItem item, User currentLeader) {
        if (!item.isAcceptingBids()) return;

        ReentrantLock lock = bidService.getLock(item.getId());
        lock.lock();
        try {
            AuctionItem freshItem = auctionService.findById(item.getId());

            if (!freshItem.isAcceptingBids()) return;

            User leader = currentLeader;
            if (leader == null) {
                List<Dto.BidResponse> history = bidService.getBidHistory(freshItem.getId());
                if (!history.isEmpty()) {
                    leader = userService.findByUsername(history.get(0).bidder().username());
                }
            }

            List<AutoBidConfig> configs = configRepo.findActiveConfigsExcluding(freshItem, leader);
            if (configs.isEmpty()) return;

            PriorityQueue<AutoBidConfig> pq = new PriorityQueue<>(
                    (c1, c2) -> c2.getMaxAmount().compareTo(c1.getMaxAmount())
            );
            pq.addAll(configs);

            while (!pq.isEmpty()) {
                AutoBidConfig winner = pq.poll();
                AutoBidConfig runnerUp = pq.peek();

                BigDecimal basePrice = freshItem.getCurrentPrice();
                if (runnerUp != null && runnerUp.getMaxAmount().compareTo(basePrice) > 0) {
                    basePrice = runnerUp.getMaxAmount();
                }

                BigDecimal nextBid = basePrice.add(winner.getIncrement());

                // Nếu nhảy vượt quá maxAmount của mình, thì chỉ đặt giá bằng đúng maxAmount
                if (nextBid.compareTo(winner.getMaxAmount()) > 0) {
                    nextBid = winner.getMaxAmount();
                }

                // Tuy nhiên, giá đặt phải luôn thỏa mãn mức giá tối thiểu của phiên
                BigDecimal minAllowedBid = freshItem.getCurrentPrice().add(freshItem.getMinIncrement());
                if (nextBid.compareTo(minAllowedBid) < 0) {
                    deactivateConfig(winner);
                    continue;
                }

                log.info("AutoBid trigger: bidder={} auction={} amount={}",
                        winner.getBidder().getUsername(), freshItem.getId(), nextBid);

                bidService.doPlaceBid(freshItem.getId(), nextBid,
                        winner.getBidder().getUsername(), true);
                break;
            }

        } finally {
            lock.unlock();
        }
    }

    @Transactional
    private void deactivateConfig(AutoBidConfig config) {
        config.setActive(false);
        configRepo.save(config);
        log.info("AutoBid maxAmount reached: bidder={} auction={}",
                config.getBidder().getUsername(), config.getAuctionItem().getId());
    }
}

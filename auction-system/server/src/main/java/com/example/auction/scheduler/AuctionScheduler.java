package com.example.auction.scheduler;

import com.example.auction.model.Auction;
import com.example.auction.model.AuctionStatus;
import com.example.auction.repository.AuctionRepository;
import com.example.auction.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuctionScheduler {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionService auctionService;

    /**
     * Chạy mỗi phút một lần để kiểm tra các phiên đấu giá đã hết thời gian.
     */
    @Scheduled(fixedRate = 60000)
    public void checkEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> endedAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.RUNNING, now);
        
        for (Auction auction : endedAuctions) {
            auctionService.endAuction(auction);
        }
    }
}

package com.example.auction.service;

import com.example.auction.model.Auction;
import com.example.auction.model.Bid;
import com.example.auction.model.User;
import com.example.auction.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class BidService {
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private UserService userService;

    public Bid placeBid(Long auctionId, Long userId, Double amount) {
        Auction auction = auctionService.getAuctionById(auctionId);
        User user = userService.findById(userId).orElse(null);
        if (auction == null || user == null) return null;
        if (amount <= auction.getCurrentPrice()) return null;
        if (auction.getEndTime().isBefore(LocalDateTime.now())) return null;

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(user);
        bid.setAmount(amount);
        bid.setTimestamp(LocalDateTime.now());
        bidRepository.save(bid);

        auctionService.updateCurrentPrice(auctionId, amount);
        return bid;
    }
}
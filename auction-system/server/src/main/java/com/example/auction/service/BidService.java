package com.example.auction.service;

import com.example.auction.model.Bid;
import com.example.auction.model.User;
import com.example.auction.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BidService {
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private UserService userService;

    @Transactional
    public Bid placeBid(Long auctionId, Long userId, Double amount) {
        User bidder = userService.findById(userId).orElse(null);
        if (bidder == null) return null;

        Bid bid = new Bid();
        bid.setBidder(bidder);
        bid.setAmount(amount);

        // Ủy quyền logic kiểm tra và cập nhật cho AuctionService
        boolean success = auctionService.placeBid(auctionId, bid);
        
        if (success) {
            return bidRepository.save(bid);
        }
        return null;
    }
}
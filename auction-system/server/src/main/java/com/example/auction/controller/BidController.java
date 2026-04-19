package com.example.auction.controller;

import com.example.auction.dto.BidRequest;
import com.example.auction.model.Bid;
import com.example.auction.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids")
public class BidController {
    @Autowired
    private BidService bidService;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
        Bid bid = bidService.placeBid(request.getAuctionId(),
                request.getUserId(),
                request.getAmount());
        if (bid == null) {
            return ResponseEntity.badRequest().body("Invalid bid");
        }
        return ResponseEntity.ok(bid);
    }
}
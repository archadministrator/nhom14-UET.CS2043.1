package com.example.auction.controller;

import com.example.auction.dto.ProxyBidRequest;
import com.example.auction.model.Auction;
import com.example.auction.model.ProxyBid;
import com.example.auction.model.User;
import com.example.auction.repository.AuctionRepository;
import com.example.auction.repository.ProxyBidRepository;
import com.example.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/proxy-bids")
public class ProxyBidController {

    @Autowired
    private ProxyBidRepository proxyBidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createProxyBid(@RequestBody ProxyBidRequest request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        ProxyBid proxyBid = new ProxyBid();
        proxyBid.setAuction(auction);
        proxyBid.setBidder(user);
        proxyBid.setMaxAmount(request.getMaxAmount());

        return ResponseEntity.ok(proxyBidRepository.save(proxyBid));
    }
}

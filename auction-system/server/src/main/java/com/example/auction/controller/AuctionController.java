package com.example.auction.controller;

import com.example.auction.dto.AuctionRequest;
import com.example.auction.model.Auction;
import com.example.auction.model.AuctionStatus;
import com.example.auction.model.User;
import com.example.auction.repository.UserRepository;
import com.example.auction.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<Auction>> getAuctions(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auctionService.getAuctions(status, query, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auction> getAuction(@PathVariable Long id) {
        Auction auction = auctionService.getAuctionById(id);
        return auction != null ? ResponseEntity.ok(auction) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody AuctionRequest request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        return ResponseEntity.ok(auctionService.createAuction(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Auction> updateAuction(@PathVariable Long id, @RequestBody AuctionRequest request, Principal principal) {
        return ResponseEntity.ok(auctionService.updateAuction(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public void deleteAuction(@PathVariable Long id, @RequestParam String adminUsername) {
        auctionService.deleteAuction(id, adminUsername);
    }
}
package com.auction.controller;

import com.auction.service.BidService;
import com.auction.util.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/{auctionId}")
    public ResponseEntity<Dto.BidResponse> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody Dto.PlaceBidRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.placeBid(auctionId, req, user.getUsername()));
    }

    @GetMapping("/{auctionId}/history")
    public ResponseEntity<List<Dto.BidResponse>> getHistory(@PathVariable Long auctionId) {
        return ResponseEntity.ok(bidService.getBidHistory(auctionId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Dto.BidResponse>> getMyBids(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bidService.getMyBids(user.getUsername()));
    }
}

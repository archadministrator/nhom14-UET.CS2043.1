package com.auction.controller;

import com.auction.service.AutoBidService;
import com.auction.util.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autobid")
@RequiredArgsConstructor
public class AutoBidController {

    private final AutoBidService autoBidService;

    @PostMapping("/{auctionId}")
    public ResponseEntity<Void> setup(
            @PathVariable Long auctionId,
            @Valid @RequestBody Dto.AutoBidRequest req,
            @AuthenticationPrincipal UserDetails user) {
        autoBidService.setupAutoBid(auctionId, req, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal UserDetails user) {
        autoBidService.cancelAutoBid(auctionId, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
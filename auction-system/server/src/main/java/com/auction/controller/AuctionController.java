package com.auction.controller;

import com.auction.model.enums.AuctionStatus;
import com.auction.service.AuctionService;
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
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public ResponseEntity<List<Dto.AuctionResponse>> getAll(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String keyword) {

        if (keyword != null && !keyword.isBlank())
            return ResponseEntity.ok(auctionService.search(keyword));
        if (status != null)
            return ResponseEntity.ok(auctionService.getByStatus(status));
        return ResponseEntity.ok(auctionService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Dto.AuctionResponse>> getActive() {
        return ResponseEntity.ok(auctionService.getActive());
    }

    @GetMapping("/my-sales")
    public ResponseEntity<List<Dto.AuctionResponse>> getMySales(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(auctionService.getMySales(user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dto.AuctionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Dto.AuctionResponse> create(
            @Valid @RequestBody Dto.CreateAuctionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.create(req, user.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dto.AuctionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody Dto.UpdateAuctionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(auctionService.update(id, req, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        auctionService.delete(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/paid")
    public ResponseEntity<Dto.AuctionResponse> markPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(auctionService.markPaid(id, user.getUsername()));
    }
}

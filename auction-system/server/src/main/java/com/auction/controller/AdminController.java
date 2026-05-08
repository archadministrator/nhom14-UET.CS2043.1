package com.auction.controller;

import com.auction.service.UserService;
import com.auction.util.Dto;
import com.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<Dto.UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable Long id,
                                          @RequestParam boolean active) {
        userService.setActive(id, active);
        return ResponseEntity.ok().build();
    }

    // ── AUCTION MANAGEMENT ───────────────────────────────────────────

    private final AuctionService auctionService;

    @GetMapping("/auctions")
    public ResponseEntity<List<Dto.AuctionResponse>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAll());
    }

    @DeleteMapping("/auctions/{id}")
    public ResponseEntity<Void> deleteAuction(@PathVariable Long id,
                                              java.security.Principal principal) {
        auctionService.delete(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/auctions/{id}/paid")
    public ResponseEntity<Dto.AuctionResponse> markPaid(@PathVariable Long id,
                                                        java.security.Principal principal) {
        return ResponseEntity.ok(auctionService.markPaid(id, principal.getName()));
    }
}
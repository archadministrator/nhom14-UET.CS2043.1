package com.auction.controller;

import com.auction.service.UserService;
import com.auction.util.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Dto.UserResponse> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.getProfile(user.getUsername()));
    }

    @PostMapping("/me/topup")
    public ResponseEntity<Dto.UserResponse> topUp(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody Dto.TopUpRequest req) {
        return ResponseEntity.ok(userService.topUpBalance(user.getUsername(), req.amount()));
    }
}
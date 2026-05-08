package com.auction.controller;

import com.auction.service.UserService;
import com.auction.util.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Dto.AuthResponse> register(@Valid @RequestBody Dto.RegisterRequest req) {
        return ResponseEntity.ok(userService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<Dto.AuthResponse> login(@Valid @RequestBody Dto.LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }
}
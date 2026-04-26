package com.example.auction.controller;

import com.example.auction.model.Rating;
import com.example.auction.model.User;
import com.example.auction.repository.RatingRepository;
import com.example.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Double> request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        user.setBalance(user.getBalance() + amount);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> rateUser(@PathVariable Long id, @RequestBody Map<String, Object> request, Principal principal) {
        User reviewer = userRepository.findByUsername(principal.getName()).orElseThrow();
        User target = userRepository.findById(id).orElseThrow();
        
        Rating rating = new Rating();
        rating.setReviewer(reviewer);
        rating.setTargetUser(target);
        rating.setScore((Integer) request.get("score"));
        rating.setComment((String) request.get("comment"));
        
        return ResponseEntity.ok(ratingRepository.save(rating));
    }

    @GetMapping("/{id}/ratings")
    public ResponseEntity<List<Rating>> getRatings(@PathVariable Long id) {
        User target = userRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(ratingRepository.findByTargetUser(target));
    }
}

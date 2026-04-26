package com.example.auction.repository;

import com.example.auction.model.Rating;
import com.example.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByTargetUser(User targetUser);
}

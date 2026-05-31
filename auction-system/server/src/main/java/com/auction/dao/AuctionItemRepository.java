package com.auction.dao;

import com.auction.model.AuctionItem;
import com.auction.model.User;
import com.auction.model.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {

    List<AuctionItem> findByStatus(AuctionStatus status);

    List<AuctionItem> findBySeller(User seller);
    List<AuctionItem> findBySellerOrderByCreatedAtDesc(User seller);

    @Query("SELECT a FROM AuctionItem a WHERE a.status = 'OPEN' AND a.startTime <= :now")
    List<AuctionItem> findItemsToActivate(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM AuctionItem a WHERE a.status = 'RUNNING' AND a.endTime <= :now")
    List<AuctionItem> findItemsToClose(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM AuctionItem a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<AuctionItem> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM AuctionItem a WHERE a.status = 'RUNNING' AND a.endTime > :now ORDER BY a.endTime ASC")
    List<AuctionItem> findActiveAuctions(@Param("now") LocalDateTime now);
}
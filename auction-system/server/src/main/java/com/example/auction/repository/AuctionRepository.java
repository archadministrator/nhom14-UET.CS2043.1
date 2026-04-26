package com.example.auction.repository;

import com.example.auction.model.Auction;
import com.example.auction.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime dateTime);

    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    @Query("SELECT a FROM Auction a WHERE LOWER(a.item.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.item.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Auction> searchAuctions(@Param("query") String query, Pageable pageable);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND (LOWER(a.item.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.item.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Auction> searchAuctionsByStatus(@Param("query") String query, @Param("status") AuctionStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Auction a SET a.currentPrice = :newPrice, a.version = a.version + 1 " +
           "WHERE a.id = :id AND a.currentPrice = :oldPrice")
    int updatePriceAtomic(@Param("id") Long id, @Param("oldPrice") Double oldPrice, @Param("newPrice") Double newPrice);
}

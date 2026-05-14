package com.auction.dao;

import com.auction.model.AuctionItem;
import com.auction.model.AutoBidConfig;
import com.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, Long> {

    Optional<AutoBidConfig> findByBidderAndAuctionItem(User bidder, AuctionItem item);

    @Query("SELECT c FROM AutoBidConfig c WHERE c.auctionItem = :item AND c.isActive = true AND c.bidder != :leader ORDER BY c.maxAmount DESC")
    List<AutoBidConfig> findActiveConfigsExcluding(@Param("item") AuctionItem item,
                                                    @Param("leader") User leader);
}
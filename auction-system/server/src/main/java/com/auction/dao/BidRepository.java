package com.auction.dao;

import com.auction.model.AuctionItem;
import com.auction.model.Bid;
import com.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionItemOrderByAmountDesc(AuctionItem item);

    List<Bid> findByBidderOrderByBidTimeDesc(User bidder);

    @Query("SELECT b FROM Bid b WHERE b.auctionItem = :item ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findTopBidByAuction(@Param("item") AuctionItem item);

    boolean existsByAuctionItemAndBidder(AuctionItem item, User bidder);

    @Query("SELECT b FROM Bid b WHERE b.auctionItem.id = :auctionId ORDER BY b.bidTime DESC")
    List<Bid> findBidHistoryByAuction(@Param("auctionId") Long auctionId);

    long countByAuctionItem(AuctionItem item);
}
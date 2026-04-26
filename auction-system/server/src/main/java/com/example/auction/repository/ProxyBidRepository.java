package com.example.auction.repository;

import com.example.auction.model.Auction;
import com.example.auction.model.ProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProxyBidRepository extends JpaRepository<ProxyBid, Long> {
    List<ProxyBid> findByAuction(Auction auction);
    Optional<ProxyBid> findByAuctionAndBidder_Id(Auction auction, Long bidderId);
}

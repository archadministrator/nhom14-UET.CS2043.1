package com.example.auction.service;

import com.example.auction.model.Auction;
import com.example.auction.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        if (auctionRepository.count() == 0) {
            Auction auction = new Auction();
            auction.setTitle("Antique Vase");
            auction.setDescription("Beautiful 19th century vase");
            auction.setStartingPrice(100.0);
            auction.setCurrentPrice(100.0);
            auction.setEndTime(LocalDateTime.now().plusDays(7));
            auction.setSeller(userService.findById(1L).orElse(null));
            auctionRepository.save(auction);

            Auction auction2 = new Auction();
            auction2.setTitle("Vintage Watch");
            auction2.setDescription("Rare mechanical watch");
            auction2.setStartingPrice(250.0);
            auction2.setCurrentPrice(250.0);
            auction2.setEndTime(LocalDateTime.now().plusDays(3));
            auction2.setSeller(userService.findById(1L).orElse(null));
            auctionRepository.save(auction2);
        }
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id).orElse(null);
    }

    public Auction updateCurrentPrice(Long auctionId, Double newPrice) {
        Auction auction = getAuctionById(auctionId);
        if (auction != null && newPrice > auction.getCurrentPrice()) {
            auction.setCurrentPrice(newPrice);
            auctionRepository.save(auction);
        }
        return auction;
    }
}
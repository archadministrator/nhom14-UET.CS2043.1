package com.auction.model;

import com.auction.model.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionItem {
    //id, seller,name, description, startprice, currentprice, minincrease, starttime, endtime, status, winner, imageURL, createdAt, bids
    private Long id;
    private User seller;
    private String name;
    private String description;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private User winner;
    private String imageUrl;
    private LocalDateTime createdAt;
    private List<Bid> bids = new ArrayList<>();

    //Constructor
    public AuctionItem(){
        this.createdAt = LocalDateTime.now();
        this.status = AuctionStatus.OPEN;
        this.minIncrement = new BigDecimal("1000");
        this.bids = new ArrayList<>();
    }

    public AuctionItem(User seller, String name, BigDecimal startPrice, LocalDateTime endTime){
        this.seller = seller;
        this.name = name;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.endTime = endTime;

        this.minIncrement = new BigDecimal("1000");
        this.status = AuctionStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.bids = new ArrayList<>();
    }

    //Getter Setter


    //Logic
    public boolean isAcceptingBids(){
        return status == AuctionStatus.RUNNING && LocalDateTime.now().isBefore(endTime);
    }

    public BigDecimal minNextBid(){return currentPrice.add(minIncrement);}
}

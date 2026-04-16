package com.auction.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.auction.model.enums.AuctionStatus;

public class AuctionItem {
    //id, seller,name, description, startprice, currentprice, minincrease, starttime, endtime, status, winner, imageURL, createdAt, bids
    private Long id;
    private User seller;
    private String name;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private User winner;
    private LocalDateTime createdAt;
    private List<Bid> bids = new ArrayList<>();

    //2 Constructor
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
    public Long getId(){return id;}
    public User getSeller(){return seller;}
    public String getName(){return name;}
    public BigDecimal getStartPrice(){return startPrice;}
    public BigDecimal getCurrentPrice(){return currentPrice;}
    public BigDecimal getMinIncrement(){return minIncrement;}
    public LocalDateTime getStartTime(){return startTime;}
    public LocalDateTime getEndTime(){return endTime;}
    public AuctionStatus getStatus(){return status;}
    public User getWinner(){return winner;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public List<Bid> getBids(){return bids;}

    public void setId(Long id){this.id = id;}
    public void setSeller(User seller){this.seller = seller;}
    public void setName(String name){this.name = name;}
    public void setStartPrice(BigDecimal startPrice){this.startPrice = startPrice;}
    public void setCurrentPrice(BigDecimal currentPrice){this.currentPrice = currentPrice;}
    public void setMinIncrement(BigDecimal minIncrement){this.minIncrement = minIncrement;}
    public void setStartTime(LocalDateTime startTime){this.startTime = startTime;}
    public void setEndTime(LocalDateTime endTime){this.endTime = endTime;}
    public void setStatus(AuctionStatus status){this.status = status;}
    public void setWinner(User winner){this.winner = winner;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}

    //Logic
    public boolean isAcceptingBids(){
        return status == AuctionStatus.RUNNING && LocalDateTime.now().isBefore(endTime);
    }

    public BigDecimal minNextBid(){return currentPrice.add(minIncrement);}
}

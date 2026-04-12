package com.auction.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {
    private Long id;
    private AuctionItem auctionItem;
    private User bidder;
    private BigDecimal amount;
    private boolean isAutoBid = false;
    private LocalDateTime bidTime = LocalDateTime.now();

    //Constructors
    public Bid(){
        this.bidTime = LocalDateTime.now();
        this.isAutoBid = false;
    }

    public Bid(Long id, AuctionItem auctionItem, User bidder, BigDecimal amount){
        this.id = id;
        this.auctionItem = auctionItem;
        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
        this.isAutoBid = false;
    }
    
    //Getter Setter
    public Long getId(){return id;}
    public AuctionItem getAuctionItem(){return auctionItem;}
    public User getBidder(){return bidder;}
    public BigDecimal getAmount(){return amount;}
    public boolean isAutoBid(){return isAutoBid;}
    public LocalDateTime getBidTime(){return bidTime;}

    public void setId(Long id){this.id = id;}
    public void setAuctionItem(AuctionItem auctionItem){this.auctionItem = auctionItem;}
    public void setBidder(User bidder){this.bidder = bidder;}
    public void setAmount(BigDecimal amount){this.amount = amount;}
    public void setIsAutoBid(boolean isAutoBid){this.isAutoBid = isAutoBid;}
    public void setBidTime(LocalDateTime bidTime){this.bidTime = bidTime;}


}

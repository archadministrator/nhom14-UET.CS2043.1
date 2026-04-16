package com.auction.service;

import com.auction.model.*;
import com.auction.model.enums.AuctionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    private List<AuctionItem> auctionItems = new ArrayList<>();
    private long sampleId = 1;

    public AuctionItem create(User seller, String name, BigDecimal startPrice, LocalDateTime startTime, LocalDateTime endTime){
        if (endTime.isBefore(startTime)){
            System.out.println("End time must after start time");
            return null;
        }

        if (endTime.isBefore(startTime.plusMinutes(15))){
            System.out.println("An auction must have a duration of 15 or longer");
            return null;
        }

        AuctionItem newItem = new AuctionItem(seller, name, startPrice, endTime);
        newItem.setStartTime(startTime);
        newItem.setId(sampleId++);

        auctionItems.add(newItem);
        return newItem;
    };

    //Here we have get all, get active auctions, get auction by id
    public List<AuctionItem> getAll(){
        return auctionItems;
    };

    public List<AuctionItem> getActive(){
        List<AuctionItem> activeAuctions = new ArrayList<>();
        for (AuctionItem item: auctionItems){
            if (item.getStatus() == AuctionStatus.RUNNING){
                activeAuctions.add(item);
            }
        }
        return activeAuctions;
    }

    public AuctionItem getById(Long id){
        for (AuctionItem item: auctionItems){
            if (item.getId().equals(id)){
                return item;
            }
        }
        return null;
    }


    // Now is update behaviors
    public boolean updateName(AuctionItem item, String newName, User user){
        if (!item.getSeller().equals(user)) return false;
        if (item.getStatus() != AuctionStatus.OPEN) return false;

        item.setName(newName);
        return true;
    }

    // Now is delete item
    public boolean delete(AuctionItem item, User user){
        if (!item.getSeller().equals(user)) return false;
        if (item.getStatus() == AuctionStatus.RUNNING) return false;

        auctionItems.remove(item);
        return true;
    }


    // Open/Close auction

    public void activateAuction(AuctionItem item){
        item.setStatus(AuctionStatus.RUNNING);
    }
    public void closeAuction(AuctionItem item){
        if (item.getBids().isEmpty()){
            item.setStatus(AuctionStatus.CANCELED);
        } else {
            item.setStatus(AuctionStatus.FINISHED);

            // Find winner, just in case status is FINISHED
            Bid highestBid = null;

            for (Bid bid: item.getBids()){
                if (highestBid == null || bid.getAmount().compareTo(highestBid.getAmount()) > 0){
                    highestBid = bid;
                }
            }

            if (highestBid != null){
                item.setWinner(highestBid.getBidder());
            }
        }
    }

}

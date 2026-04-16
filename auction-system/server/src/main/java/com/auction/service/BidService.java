package com.auction.service;

import com.auction.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidService {
    
    public boolean placeBid(User user, AuctionItem item, BigDecimal amount){
        // Các điều kiện không hợp lệ: user hoặc item không tồn tại; seller tự đặt bid cho sản phẩm của chính mình; giá đặt nhỏ hơn giá tăng tối thiểu; số dư không đủ
        if (user == null || item == null) return false;
        if (!item.isAcceptingBids()) {
            System.out.println("Auction closed");
            return false;
        }

        if (item.getSeller().equals(user)){  
            System.out.println("Seller cannot bid own item ?");
            return false;
        }

        BigDecimal minBid = item.minNextBid();
        if (amount.compareTo(minBid) < 0){
            System.out.println("Invalid bid");
            return false;
        }
        if (user.getBalance().compareTo(amount) < 0){
            System.out.println("Your balance is so poor, bratha");
            return false;
        }

        //Thoả mãn tất cả, bắt đầu tạo đặt bid
        Bid bid = new Bid(null, item, user, amount);
        bid.setBidTime(LocalDateTime.now());

        item.getBids().add(bid);
        item.setCurrentPrice(amount);
        item.setWinner(user);

        user.setBalance(user.getBalance().subtract(amount));
        System.out.println("Bidded successfully: " + user.getUsername() + " - " + amount);
        return true;


    }
}

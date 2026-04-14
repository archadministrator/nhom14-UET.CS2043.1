package com.auction.service;

import com.auction.model.*;

import java.math.BigDecimal;

public class BidService {
    
    public boolean placeBid(User user, AuctionItem item, BigDecimal amount){
        // Các điều kiện 
        if (user == null || item == null) return false;
        if (!item.isAcceptingBids()) {
            System.out.println("Phiên đấu giá đã đóng");
            return false;
        }

        if (item.getSeller().equals(user)){  
            System.out.println("Seller mà tự đấu giá sản phẩm của mình ?");
            return false;
        }

        BigDecimal minBid = item.minNextBid();
        if (amount.compareTo(minBid) < 0){
            System.out.println("Giá đặt không hợp lệ");
            return false;
        }
        if (user.getBalance().compareTo(amount) < 0){
            System.out.println("Số dư không đủ để đặt");
            return false;
        }

        //Thoả mãn tất cả, bắt đầu tạo đặt bid
        Bid bid = new Bid(null, item, user, amount);

        boolean success = item.placeBid(bid);
        if (success){
            user.withdraw(amount);
        } 
        return success;



    }
}

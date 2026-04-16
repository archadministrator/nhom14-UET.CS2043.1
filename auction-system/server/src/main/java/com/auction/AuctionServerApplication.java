package com.auction;

import com.auction.model.*;
import com.auction.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuctionServerApplication {
    public static void main(String[] args){
        UserService userService = new UserService();
        AuctionService auctionService = new AuctionService();
        BidService bidService = new BidService();


        User user1 = userService.register("Nguyen Van A", "nguyenvana@gmail.com", "12345678A");
        User user2 = userService.register("NguyenVanB", "nguyenvanb@gmail.com", "12345678B");

        userService.topUp(user1, new BigDecimal(10000));
        userService.topUp(user2, new BigDecimal(100000000));

        user1.getBalance();
        user2.getBalance();

        AuctionItem item = auctionService.create(user1, "Mac Book Air", new BigDecimal(1000), LocalDateTime.now(), LocalDateTime.of(2026, 12, 31, 23, 59));
        auctionService.activateAuction(item);

        bidService.placeBid(user2, item, new BigDecimal(1500));
    }
}
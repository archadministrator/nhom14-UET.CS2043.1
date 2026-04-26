package com.example.auction;

import com.example.auction.service.UserService;
import com.example.auction.service.AuctionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class AuctionApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuctionApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserService userService, AuctionService auctionService) {
        return args -> {
            userService.initializeData();
            auctionService.initializeData();
        };
    }
}
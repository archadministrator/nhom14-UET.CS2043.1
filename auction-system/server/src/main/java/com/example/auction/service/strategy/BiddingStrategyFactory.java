package com.example.auction.service.strategy;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

/**
 * Lớp triển khai Factory Pattern để quản lý việc cung cấp các chiến lược đấu giá.
 */
@Component
public class BiddingStrategyFactory {

    private final Map<String, BiddingStrategy> strategies = new HashMap<>();

    /**
     * Map các bean strategy vào Factory dựa trên tên.
     */
    public BiddingStrategyFactory(EnglishBiddingStrategy englishStrategy, 
                                  DutchBiddingStrategy dutchStrategy) {
        strategies.put("ENGLISH", englishStrategy);
        strategies.put("DUTCH", dutchStrategy);
    }

    /**
     * Trả về chiến lược tương ứng với loại cuộc đấu giá.
     */
    public BiddingStrategy getStrategy(String type) {
        BiddingStrategy strategy = strategies.get(type != null ? type.toUpperCase() : "ENGLISH");
        if (strategy == null) {
            return strategies.get("ENGLISH"); // Mặc định là English
        }
        return strategy;
    }
}

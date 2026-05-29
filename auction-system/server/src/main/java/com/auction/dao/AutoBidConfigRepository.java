package com.auction.dao;

import com.auction.model.AuctionItem;
import com.auction.model.AutoBidConfig;
import com.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, Long> {

    Optional<AutoBidConfig> findByBidderAndAuctionItem(User bidder, AuctionItem item);

    /**
     * Trả về tất cả config active của phiên, NGOẠI TRỪ người đang dẫn đầu,
     * sắp xếp theo createdAt ASC — người đăng ký auto-bid SỚM hơn được ưu tiên.
     *
     * Việc sắp xếp ở DB thay vì trong PriorityQueue giúp tránh load toàn bộ
     * dữ liệu lên memory rồi mới sort.
     */
    @Query("""
            SELECT c FROM AutoBidConfig c
            WHERE c.auctionItem = :item
              AND c.isActive = true
              AND (:leader IS NULL OR c.bidder != :leader)
            ORDER BY c.createdAt ASC
            """)
    List<AutoBidConfig> findActiveConfigsExcluding(@Param("item") AuctionItem item,
                                                   @Param("leader") User leader);
}
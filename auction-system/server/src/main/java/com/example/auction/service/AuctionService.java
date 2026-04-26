package com.example.auction.service;

import com.example.auction.dto.AuctionRequest;
import com.example.auction.model.*;
import com.example.auction.repository.AuctionRepository;
import com.example.auction.repository.ProxyBidRepository;
import com.example.auction.repository.UserRepository;
import com.example.auction.service.observer.AuctionEvent;
import com.example.auction.service.observer.NotificationService;
import com.example.auction.service.strategy.BiddingStrategy;
import com.example.auction.service.strategy.BiddingStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionService {
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProxyBidRepository proxyBidRepository;

    private final BiddingStrategyFactory strategyFactory;

    public AuctionService(BiddingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    // Bỏ @PostConstruct
    public void initializeData() {
        try {
            if (auctionRepository.count() == 0) {
                User seller = userService.findById(2L).orElse(null);

                // Tạo đấu giá đồ điện tử
                Auction a1 = new Auction();
                Item e1 = ItemFactory.createItem("ELECTRONICS", "iPhone 15 Pro", "Mới 99%, đầy đủ phụ kiện");
                if (e1 instanceof Electronics elec) {
                    elec.setBrand("Apple");
                    elec.setModel("15 Pro");
                }
                a1.setItem(e1);
                a1.setStartingPrice(1000.0);
                a1.setCurrentPrice(1000.0);
                a1.setStartTime(LocalDateTime.now());
                a1.setEndTime(LocalDateTime.now().plusDays(1));
                a1.setStatus(AuctionStatus.RUNNING);
                a1.setSeller(seller);
                auctionRepository.save(a1);

                // Tạo đấu giá tác phẩm nghệ thuật
                Auction a2 = new Auction();
                Item art1 = ItemFactory.createItem("ART", "Tranh sơn mài", "Tác phẩm của nghệ nhân nổi tiếng");
                if (art1 instanceof Art art) {
                    art.setArtist("Nguyễn Phan Chánh");
                    art.setYearCreated(1930);
                }
                a2.setItem(art1);
                a2.setStartingPrice(5000.0);
                a2.setCurrentPrice(5000.0);
                a2.setStartTime(LocalDateTime.now());
                a2.setEndTime(LocalDateTime.now().plusHours(5));
                a2.setStatus(AuctionStatus.RUNNING);
                a2.setSeller(seller);
                auctionRepository.save(a2);
                System.out.println(">>> Đã khởi tạo dữ liệu đấu giá mẫu.");
            }
        } catch (Exception e) {
            System.err.println(">>> Không thể khởi tạo dữ liệu đấu giá: " + e.getMessage());
        }
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public Page<Auction> getAuctions(AuctionStatus status, String query, Pageable pageable) {
        if (query != null && !query.isEmpty()) {
            if (status != null) {
                return auctionRepository.searchAuctionsByStatus(query, status, pageable);
            }
            return auctionRepository.searchAuctions(query, pageable);
        }
        if (status != null) {
            return auctionRepository.findByStatus(status, pageable);
        }
        return auctionRepository.findAll(pageable);
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id).orElse(null);
    }

    @Transactional
    public Auction createAuction(AuctionRequest request, User seller) {
        Auction auction = new Auction();
        Item item = ItemFactory.createItem(request.getItemType(), request.getItemName(), request.getDescription());
        auction.setItem(item);
        auction.setStartingPrice(request.getStartingPrice());
        auction.setCurrentPrice(request.getStartingPrice());
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(request.getEndTime());
        auction.setSeller(seller);
        auction.setStatus(AuctionStatus.RUNNING);
        
        return auctionRepository.save(auction);
    }

    @Transactional
    public Auction updateAuction(Long id, AuctionRequest request, String username) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
        
        if (!auction.getSeller().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to update this auction");
        }
        
        if (auction.getBids() != null && !auction.getBids().isEmpty()) {
            throw new RuntimeException("Cannot update auction that has bids");
        }

        auction.getItem().setName(request.getItemName());
        auction.getItem().setDescription(request.getDescription());
        auction.setStartingPrice(request.getStartingPrice());
        auction.setEndTime(request.getEndTime());
        
        return auctionRepository.save(auction);
    }

    @Transactional
    public boolean placeBid(Long auctionId, Bid bid) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return false;

        // Kiểm tra thời gian
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            endAuction(auction);
            return false;
        }

        BiddingStrategy strategy = strategyFactory.getStrategy("ENGLISH"); // Mặc định English
        
        if (!strategy.processBid(auction, bid)) {
            return false;
        }

        // Logic Anti-sniping: Nếu bid trong 30 giây cuối, cộng thêm 60 giây (Y=60)
        LocalDateTime now = LocalDateTime.now();
        if (now.plusSeconds(30).isAfter(auction.getEndTime())) {
            auction.setEndTime(auction.getEndTime().plusSeconds(60));
        }

        // Lưu Bid và cập nhật Auction
        bid.setAuction(auction);
        bid.setTimestamp(now);
        auction.setWinner(bid.getBidder());
        auctionRepository.save(auction); // JPA @Version sẽ handle concurrency ở đây

        notificationService.notifyObservers(AuctionEvent.NEW_BID, auction);

        // Xử lý Proxy Bidding
        handleProxyBidding(auction);

        return true;
    }

    private void handleProxyBidding(Auction auction) {
        List<ProxyBid> proxies = proxyBidRepository.findByAuction(auction);
        for (ProxyBid proxy : proxies) {
            // Nếu người đặt proxy không phải là người đang dẫn đầu và giá trần của họ cao hơn giá hiện tại
            if (!proxy.getBidder().equals(auction.getWinner()) && proxy.getMaxAmount() > auction.getCurrentPrice()) {
                Double nextBidAmount = auction.getCurrentPrice() + 50.0; // Bước giá ví dụ là 50
                if (nextBidAmount <= proxy.getMaxAmount()) {
                    Bid autoBid = new Bid();
                    autoBid.setAmount(nextBidAmount);
                    autoBid.setBidder(proxy.getBidder());
                    autoBid.setIdempotencyKey(UUID.randomUUID().toString());
                    
                    // Gọi đặt giá (Lưu ý: đặt trực tiếp để tránh đệ quy quá sâu hoặc dùng Queue)
                    placeBidInternal(auction, autoBid);
                }
            }
        }
    }

    private void placeBidInternal(Auction auction, Bid bid) {
        bid.setAuction(auction);
        bid.setTimestamp(LocalDateTime.now());
        auction.setCurrentPrice(bid.getAmount());
        auction.setWinner(bid.getBidder());
        auctionRepository.save(auction);
        notificationService.notifyObservers(AuctionEvent.NEW_BID, auction);
    }

    @Transactional
    public void endAuction(Auction auction) {
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            auction.setStatus(AuctionStatus.FINISHED);
            
            // Xử lý tài chính
            if (auction.getWinner() != null) {
                User winner = auction.getWinner();
                User seller = auction.getSeller();
                Double finalPrice = auction.getCurrentPrice();
                
                if (winner.getBalance() >= finalPrice) {
                    winner.setBalance(winner.getBalance() - finalPrice);
                    seller.setBalance(seller.getBalance() + finalPrice);
                    userRepository.save(winner);
                    userRepository.save(seller);
                }
            }

            auctionRepository.save(auction);
            notificationService.notifyObservers(AuctionEvent.AUCTION_ENDED, auction);
            
            auditService.logAction("Auction", auction.getId(), AuditLog.ActionType.STATE_CHANGE, 
                "SYSTEM", "Phiên đấu giá kết thúc. Người thắng: " + 
                (auction.getWinner() != null ? auction.getWinner().getUsername() : "Không có"));
        }
    }

    @Transactional
    public void deleteAuction(Long auctionId, String adminUsername) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction != null) {
            auction.setStatus(AuctionStatus.CANCELED);
            auctionRepository.save(auction);
            auditService.logAction("Auction", auctionId, AuditLog.ActionType.SOFT_DELETE, 
                adminUsername, "Hủy phiên đấu giá: " + auction.getItem().getName());
        }
    }
}

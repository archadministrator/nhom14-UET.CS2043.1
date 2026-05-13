package com.auction.service;
import com.auction.dao.AuctionItemRepository;
import com.auction.dao.BidRepository;
import com.auction.exception.AccessDeniedException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.model.AuctionItem;
import com.auction.model.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.util.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuctionService {
    private final AuctionItemRepository auctionRepo;
    private final BidRepository bidRepo;
    private final UserService userService;

    public AuctionService(AuctionItemRepository auctionRepo, BidRepository bidRepo, UserService userService) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
        this.userService = userService;
    }

    @Transactional
    public Dto.AuctionResponse create(Dto.CreateAuctionRequest req, String sellerUsername) {
        User seller = userService.findByUsername(sellerUsername);

        if (req.endTime().isBefore(req.startTime()))
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        if (req.endTime().isBefore(req.startTime().plusMinutes(5)))
            throw new IllegalArgumentException("Phiên đấu giá phải kéo dài ít nhất 5 phút.");

        AuctionItem item = AuctionItem.builder()
                .seller(seller)
                .name(req.name())
                .description(req.description())
                .startPrice(req.startPrice())
                .currentPrice(req.startPrice())
                .minIncrement(req.minIncrement())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .imageUrl(req.imageUrl())
                .status(AuctionStatus.OPEN)
                .build();

        AuctionItem saved = auctionRepo.save(item);
        log.info("Auction tạo mới: [{}] '{}' bởi {}", saved.getId(), saved.getName(), sellerUsername);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Dto.AuctionResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<Dto.AuctionResponse> getAll() {
        return auctionRepo.findAll().stream().map(item -> toResponse(item)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Dto.AuctionResponse> getActive() {
        return auctionRepo.findActiveAuctions(LocalDateTime.now())
                .stream().map(item -> toResponse(item)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Dto.AuctionResponse> getByStatus(AuctionStatus status) {
        return auctionRepo.findByStatus(status).stream().map(item -> toResponse(item)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Dto.AuctionResponse> getMySales(String sellerUsername) {
        User seller = userService.findByUsername(sellerUsername);
        return auctionRepo.findBySellerOrderByCreatedAtDesc(seller)
                .stream().map(item -> toResponse(item)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Dto.AuctionResponse> search(String keyword) {
        return auctionRepo.searchByKeyword(keyword).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Dto.AuctionResponse update(Long id, Dto.UpdateAuctionRequest req, String username) {
        AuctionItem item = findById(id);
        assertSellerOrAdmin(item, username);

        if (item.getStatus() != AuctionStatus.OPEN)
            throw new IllegalStateException("Chỉ có thể chỉnh sửa phiên ở trạng thái OPEN.");

        if (req.name() != null) item.setName(req.name());
        if (req.description() != null) item.setDescription(req.description());
        if (req.imageUrl() != null) item.setImageUrl(req.imageUrl());

        return toResponse(auctionRepo.save(item));
    }

    @Transactional
    public void delete(Long id, String username) {
        AuctionItem item = findById(id);
        assertSellerOrAdmin(item, username);

        // 1. Không cho xóa nếu đã kết thúc hoặc đã trả tiền
        if (item.getStatus() == AuctionStatus.FINISHED || item.getStatus() == AuctionStatus.PAID)
            throw new IllegalStateException("Không thể hủy phiên đã kết thúc hoặc đã thanh toán.");

        // 2. Nếu đang diễn ra (RUNNING), phải hoàn tiền cho người giữ giá cao nhất
        if (item.getStatus() == AuctionStatus.RUNNING) {
            bidRepo.findTopBidByAuction(item).ifPresent(topBid -> {
                userService.addBalance(topBid.getBidder().getUsername(), topBid.getAmount());
                log.info("Hoàn tiền cho người giữ giá cao nhất [{}] khi hủy phiên: {}", 
                        topBid.getBidder().getUsername(), topBid.getAmount());
            });
        }

        // 3. Thực hiện Xóa mềm (Soft Delete)
        item.setStatus(AuctionStatus.CANCELED);
        auctionRepo.save(item);
        log.info("Auction [{}] đã bị HỦY (Soft Delete) bởi {}", id, username);
    }

    @Transactional
    public void activateAuction(AuctionItem item) {
        item.setStatus(AuctionStatus.RUNNING);
        auctionRepo.save(item);
        log.info("Auction [{}] '{}' → RUNNING", item.getId(), item.getName());
    }

    @Transactional
    public void closeAuction(AuctionItem item) {
        long bidCount = bidRepo.countByAuctionItem(item);

        if (bidCount == 0) {
            item.setStatus(AuctionStatus.CANCELED);
            log.info("Auction [{}] '{}' → CANCELED (không có bid)", item.getId(), item.getName());
        } else {
            bidRepo.findTopBidByAuction(item).ifPresent(topBid -> {
                item.setWinner(topBid.getBidder());
                log.info("Auction [{}] winner: {} với giá {}",
                        item.getId(), topBid.getBidder().getUsername(), topBid.getAmount());
            });
            item.setStatus(AuctionStatus.FINISHED);
            log.info("Auction [{}] '{}' → FINISHED", item.getId(), item.getName());
        }
        auctionRepo.save(item);
    }

    @Transactional
    public Dto.AuctionResponse markPaid(Long id, String adminUsername) {
        AuctionItem item = findById(id);
        if (item.getStatus() != AuctionStatus.FINISHED)
            throw new IllegalStateException("Chỉ có thể đánh dấu PAID khi phiên ở trạng thái FINISHED.");
        item.setStatus(AuctionStatus.PAID);
        log.info("Auction [{}] → PAID bởi {}", id, adminUsername);
        return toResponse(auctionRepo.save(item));
    }

    public AuctionItem findById(Long id) {
        return auctionRepo.findById(id)
                .orElseThrow(() -> new AuctionNotFoundException(id));
    }

    public List<AuctionItem> findItemsToActivate() {
        return auctionRepo.findItemsToActivate(LocalDateTime.now());
    }

    public List<AuctionItem> findItemsToClose() {
        return auctionRepo.findItemsToClose(LocalDateTime.now());
    }

    private void assertSellerOrAdmin(AuctionItem item, String username) {
        User user = userService.findByUsername(username);
        boolean isSeller = item.getSeller().getUsername().equals(username);
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        if (!isSeller && !isAdmin)
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
    }

    private Dto.AuctionResponse toResponse(AuctionItem item) {
        long bidCount = bidRepo.countByAuctionItem(item);
        return Dto.AuctionResponse.from(item, bidCount);
    }
}

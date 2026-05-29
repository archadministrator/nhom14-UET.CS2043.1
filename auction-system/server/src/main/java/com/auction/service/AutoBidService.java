package com.auction.service;

import com.auction.dao.AutoBidConfigRepository;
import com.auction.model.AuctionItem;
import com.auction.model.AutoBidConfig;
import com.auction.model.User;
import com.auction.util.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class AutoBidService {

    private final AutoBidConfigRepository configRepo;
    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public AutoBidService(AutoBidConfigRepository configRepo,
                          UserService userService,
                          AuctionService auctionService,
                          @Lazy BidService bidService) {
        this.configRepo = configRepo;
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    @Transactional
    public AutoBidConfig setupAutoBid(Long auctionId, Dto.AutoBidRequest req, String username) {
        User bidder = userService.findByUsername(username);
        AuctionItem item = auctionService.findById(auctionId);

        if (!item.isAcceptingBids())
            throw new IllegalStateException("Phiên đấu giá không đang chạy.");

        if (req.maxAmount().compareTo(item.getCurrentPrice()) <= 0)
            throw new IllegalArgumentException("Giá tối đa phải lớn hơn giá hiện tại.");

        AutoBidConfig config = configRepo
                .findByBidderAndAuctionItem(bidder, item)
                .orElse(AutoBidConfig.builder().bidder(bidder).auctionItem(item).build());

        config.setMaxAmount(req.maxAmount());
        config.setIncrement(req.increment());
        config.setActive(true);

        AutoBidConfig saved = configRepo.save(config);
        log.info("AutoBid setup: bidder={} auction={} maxAmount={}",
                username, auctionId, req.maxAmount());

        triggerAutoBids(item, null);
        return saved;
    }

    @Transactional
    public void cancelAutoBid(Long auctionId, String username) {
        User bidder = userService.findByUsername(username);
        AuctionItem item = auctionService.findById(auctionId);

        configRepo.findByBidderAndAuctionItem(bidder, item).ifPresent(cfg -> {
            cfg.setActive(false);
            configRepo.save(cfg);
            log.info("AutoBid cancelled: bidder={} auction={}", username, auctionId);
        });
    }

    /**
     * Kích hoạt auto-bid sau mỗi lần có bid mới (thủ công hoặc auto).
     *
     * Thuật toán dùng PriorityQueue để xử lý đúng thứ tự ưu tiên khi nhiều
     * auto-bidder cùng đủ điều kiện:
     *
     *   Ưu tiên 1 — maxAmount CAO hơn thắng (sẵn sàng trả nhiều hơn)
     *   Ưu tiên 2 — Nếu maxAmount bằng nhau, người đăng ký SỚM hơn thắng (FIFO)
     *
     * Ví dụ:
     *   Giá hiện tại: 100, increment: 10
     *   Alice  đăng ký auto-bid lúc T=1, maxAmount=200
     *   Bob    đăng ký auto-bid lúc T=2, maxAmount=180
     *   Charlie đăng ký auto-bid lúc T=3, maxAmount=200
     *
     *   → PriorityQueue poll(): Alice (maxAmount=200, sớm nhất)
     *   → Alice đặt 110, vẫn dưới maxAmount=200 → tiếp tục
     *   → Loop: Bob và Charlie vẫn trong queue nhưng Alice đang dẫn đầu
     *     → Không trigger thêm (leader bị loại khỏi query)
     */
    public void triggerAutoBids(AuctionItem item, User currentLeader) {
        if (!item.isAcceptingBids()) return;

        // Lấy lock của phiên — cùng lock với BidService.doPlaceBid()
        // ReentrantLock cho phép thread đang giữ lock acquire lại → không deadlock
        ReentrantLock lock = bidService.getLock(item.getId());
        lock.lock();
        try {
            AuctionItem freshItem = auctionService.findById(item.getId());
            if (!freshItem.isAcceptingBids()) return;

            // Xác định người đang dẫn đầu để loại khỏi danh sách ứng viên
            User leader = resolveLeader(currentLeader, freshItem);

            // Lấy danh sách active configs (đã ORDER BY createdAt ASC từ DB)
            List<AutoBidConfig> configs = configRepo.findActiveConfigsExcluding(freshItem, leader);
            if (configs.isEmpty()) return;

            // Đưa vào PriorityQueue với comparator ưu tiên: maxAmount DESC, createdAt ASC
            PriorityQueue<AutoBidConfig> queue = buildPriorityQueue(configs);

            // Poll ứng viên có độ ưu tiên cao nhất (maxAmount DESC, createdAt ASC)
            AutoBidConfig best = queue.poll();
            if (best == null) return;

            // ── Proxy Bidding Logic ──────────────────────────────────────────
            // Không đặt giá dư thừa. Nếu best có maxAmount cao hơn đối thủ bị loại
            // (loser — người đang dẫn đầu hoặc ứng viên ưu tiên thấp hơn), best chỉ
            // cần đặt đúng loser.maxAmount + best.increment để vượt qua.
            //
            // Ví dụ:
            //   currentPrice = 2_000_000
            //   bidder01 (loser, đang dẫn): maxAmount = 3_000_000, increment = 200_000
            //   bidder02 (best):            maxAmount = 5_000_000, increment = 200_000
            //   → nextBid = 3_000_000 + 200_000 = 3_200_000  (không phải 2_200_000)
            //
            // Nếu không có ứng viên thua (queue rỗng sau khi poll best), dùng:
            //   nextBid = currentPrice + best.increment  (bid tối thiểu hợp lệ)
            // ─────────────────────────────────────────────────────────────────
            AutoBidConfig loser = queue.peek(); // ứng viên ưu tiên kế tiếp (có thể null)
            BigDecimal nextBid = computeProxyBid(freshItem.getCurrentPrice(), best, loser);

            if (nextBid.compareTo(best.getMaxAmount()) > 0) {
                // best cũng không đủ maxAmount để đặt → deactivate, không trigger
                log.info("AutoBid maxAmount reached: bidder={} auction={} maxAmount={}",
                        best.getBidder().getUsername(), freshItem.getId(), best.getMaxAmount());
                deactivateConfig(best);
                return;
            }

            log.info("AutoBid trigger: bidder={} auction={} amount={}",
                    best.getBidder().getUsername(), freshItem.getId(), nextBid);

            // doPlaceBid là @Transactional + đã giữ lock (reentrant) → an toàn
            bidService.doPlaceBid(freshItem.getId(), nextBid,
                    best.getBidder().getUsername(), true);

        } finally {
            lock.unlock();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tính nextBid theo proxy bidding:
     *   - Nếu có loser (ứng viên kém ưu tiên hơn trong queue):
     *       nextBid = loser.maxAmount + best.increment
     *       → vừa đủ để vượt loser, không trả thừa
     *   - Nếu không có loser (best là duy nhất):
     *       nextBid = currentPrice + best.increment
     *       → bid tối thiểu hợp lệ
     *
     * Kết quả được cap tại best.maxAmount (kiểm tra sau khi gọi hàm này).
     */
    private BigDecimal computeProxyBid(BigDecimal currentPrice,
                                       AutoBidConfig best,
                                       AutoBidConfig loser) {
        if (loser != null) {
            // Proxy bid: vượt maxAmount của loser bằng increment của best
            BigDecimal proxyBid = loser.getMaxAmount().add(best.getIncrement());
            // Không được thấp hơn mức tối thiểu hợp lệ (currentPrice + increment)
            BigDecimal minBid = currentPrice.add(best.getIncrement());
            return proxyBid.max(minBid);
        }
        // Không có đối thủ → đặt tối thiểu
        return currentPrice.add(best.getIncrement());
    }

    /**
     * Xây PriorityQueue với thứ tự ưu tiên:
     *   1. maxAmount DESC — trả nhiều hơn được ưu tiên
     *   2. createdAt ASC  — đăng ký sớm hơn được ưu tiên (tie-break)
     */
    private PriorityQueue<AutoBidConfig> buildPriorityQueue(List<AutoBidConfig> configs) {
        Comparator<AutoBidConfig> comparator = Comparator
                .comparing(AutoBidConfig::getMaxAmount).reversed()  // cao hơn → ưu tiên hơn
                .thenComparing(AutoBidConfig::getCreatedAt);        // sớm hơn → ưu tiên hơn

        PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>(comparator);
        queue.addAll(configs);
        return queue;
    }

    /**
     * Xác định người đang dẫn đầu phiên.
     * Nếu currentLeader được truyền vào (vừa đặt bid), dùng luôn.
     * Nếu không (ví dụ: gọi từ setupAutoBid), tra cứu từ lịch sử bid.
     */
    private User resolveLeader(User currentLeader, AuctionItem item) {
        if (currentLeader != null) return currentLeader;

        List<Dto.BidResponse> history = bidService.getBidHistory(item.getId());
        if (history.isEmpty()) return null;

        return userService.findByUsername(history.get(0).bidder().username());
    }

    @Transactional
    private void deactivateConfig(AutoBidConfig config) {
        config.setActive(false);
        configRepo.save(config);
    }
}
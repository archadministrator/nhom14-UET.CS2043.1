package com.auction.service;

import com.auction.dao.BidRepository;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionItem;
import com.auction.model.Bid;
import com.auction.model.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.Role;
import com.auction.util.Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidService Tests")
class BidServiceTest {

    @Mock BidRepository bidRepo;
    @Mock AuctionService auctionService;
    @Mock UserService userService;
    @Mock AutoBidService autoBidService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks BidService bidService;

    private User seller;
    private User bidder;
    private User previousBidder;
    private AuctionItem activeAuction;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(1L).username("seller01")
                .email("s@test.com").role(Role.SELLER).balance(BigDecimal.ZERO).build();

        bidder = User.builder().id(2L).username("bidder01")
                .email("b@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("5000000")).build();

        previousBidder = User.builder().id(3L).username("prev_bidder")
                .email("prev@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("2000000")).build();

        activeAuction = AuctionItem.builder()
                .id(10L)
                .seller(seller)
                .name("Đồng hồ Rolex")
                .startPrice(new BigDecimal("10000000"))
                .currentPrice(new BigDecimal("10000000"))
                .minIncrement(new BigDecimal("500000"))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.RUNNING)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // doPlaceBid — HAPPY PATH
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("doPlaceBid() — happy path")
    class DoPlaceBidSuccess {

        @Test
        @DisplayName("Bid hợp lệ, không có người dẫn đầu trước → đặt giá thành công")
        void placeBid_noTopBidder_success() {
            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            Dto.BidResponse result = bidService.doPlaceBid(10L, bidAmount, "bidder01", false);

            assertThat(result.amount()).isEqualByComparingTo(bidAmount);
            assertThat(result.isAutoBid()).isFalse();
            then(userService).should().subtractBalance("bidder01", bidAmount);
            then(userService).should(never()).addBalance(anyString(), any());
        }

        @Test
        @DisplayName("Bid hợp lệ, có người dẫn đầu trước → hoàn tiền cho người cũ")
        void placeBid_withPreviousBidder_refundsPrevious() {
            BigDecimal newBidAmount = new BigDecimal("11000000");
            Bid prevBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(previousBidder).amount(new BigDecimal("10500000")).build();
            Bid savedBid = Bid.builder().id(2L).auctionItem(activeAuction)
                    .bidder(bidder).amount(newBidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.of(prevBid));
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(2L);

            bidService.doPlaceBid(10L, newBidAmount, "bidder01", false);

            then(userService).should().addBalance("prev_bidder", new BigDecimal("10500000"));
        }

        @Test
        @DisplayName("Bid đúng minimum → chấp nhận")
        void placeBid_exactMinimum_accepted() {
            BigDecimal exactMin = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(exactMin).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            assertThatCode(() -> bidService.doPlaceBid(10L, exactMin, "bidder01", false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Auto-bid → BidResponse.isAutoBid = true")
        void placeBid_autoBid_flaggedCorrectly() {
            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(3L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).isAutoBid(true).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            Dto.BidResponse result = bidService.doPlaceBid(10L, bidAmount, "bidder01", true);

            assertThat(result.isAutoBid()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // doPlaceBid — VALIDATION ERRORS
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("doPlaceBid() — validation errors")
    class DoPlaceBidValidation {

        @Test
        @DisplayName("Phiên đã đóng (FINISHED) → ném AuctionClosedException")
        void placeBid_auctionFinished_throws() {
            activeAuction.setStatus(AuctionStatus.FINISHED);
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, new BigDecimal("11000000"), "bidder01", false))
                    .isInstanceOf(AuctionClosedException.class);
        }

        @Test
        @DisplayName("Phiên hết giờ dù đang RUNNING → ném AuctionClosedException")
        void placeBid_expiredAuction_throws() {
            activeAuction.setEndTime(LocalDateTime.now().minusMinutes(1));
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, new BigDecimal("11000000"), "bidder01", false))
                    .isInstanceOf(AuctionClosedException.class);
        }

        @Test
        @DisplayName("Seller tự đặt giá sản phẩm của mình → ném InvalidBidException")
        void placeBid_sellerBidsOwnAuction_throws() {
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, new BigDecimal("11000000"), "seller01", false))
                    .isInstanceOf(InvalidBidException.class)
                    .hasMessageContaining("Người bán");
        }

        @Test
        @DisplayName("Giá đặt thấp hơn minimum → ném InvalidBidException")
        void placeBid_amountBelowMinimum_throws() {
            BigDecimal tooLow = new BigDecimal("10000001");
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, tooLow, "bidder01", false))
                    .isInstanceOf(InvalidBidException.class)
                    .hasMessageContaining("tối thiểu");
        }

        @Test
        @DisplayName("Số dư không đủ → ném exception, không lưu bid")
        void placeBid_insufficientBalance_throws() {
            BigDecimal bidAmount = new BigDecimal("10500000");
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            willThrow(new RuntimeException("Số dư không đủ."))
                    .given(userService).subtractBalance("bidder01", bidAmount);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, bidAmount, "bidder01", false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Số dư không đủ");

            then(bidRepo).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ANTI-SNIPING
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Anti-sniping extension")
    class AntiSniping {

        @Test
        @DisplayName("Bid trong 5 phút cuối → endTime gia hạn thêm 5 phút")
        void placeBid_last5Minutes_extendsEndTime() {
            LocalDateTime nearEnd = LocalDateTime.now().plusMinutes(3);
            activeAuction.setEndTime(nearEnd);

            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            bidService.doPlaceBid(10L, bidAmount, "bidder01", false);

            assertThat(activeAuction.getEndTime()).isAfter(nearEnd);
        }

        @Test
        @DisplayName("Bid khi còn nhiều thời gian → endTime không thay đổi")
        void placeBid_earlyBid_endTimeUnchanged() {
            LocalDateTime originalEnd = LocalDateTime.now().plusHours(2);
            activeAuction.setEndTime(originalEnd);

            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            bidService.doPlaceBid(10L, bidAmount, "bidder01", false);

            assertThat(activeAuction.getEndTime()).isEqualTo(originalEnd);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // LOCKING — per-auction isolation
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Per-auction locking")
    class LockingTests {

        @Test
        @DisplayName("getLock() trả về cùng lock instance cho cùng auctionId")
        void getLock_sameId_returnsSameInstance() {
            ReentrantLock lock1 = bidService.getLock(10L);
            ReentrantLock lock2 = bidService.getLock(10L);
            assertThat(lock1).isSameAs(lock2);
        }

        @Test
        @DisplayName("getLock() trả về lock khác nhau cho auctionId khác nhau")
        void getLock_differentId_returnsDifferentInstances() {
            ReentrantLock lockA = bidService.getLock(10L);
            ReentrantLock lockB = bidService.getLock(20L);
            assertThat(lockA).isNotSameAs(lockB);
        }

        @Test
        @DisplayName("Lock là ReentrantLock fair=true")
        void getLock_isFair() {
            ReentrantLock lock = bidService.getLock(99L);
            assertThat(lock.isFair()).isTrue();
        }

        @Test
        @DisplayName("Hai phiên khác nhau không block lẫn nhau (lock độc lập)")
        void locks_differentAuctions_doNotBlock() throws InterruptedException {
            // Giữ lock phiên 10 trong thread riêng
            ReentrantLock lockA = bidService.getLock(10L);
            CountDownLatch held = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(1);

            Thread t = new Thread(() -> {
                lockA.lock();
                held.countDown();           // báo đã giữ lock
                try { done.await(3, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                lockA.unlock();
            });
            t.start();
            held.await(1, TimeUnit.SECONDS);

            // Lock phiên 20 phải lấy được ngay lập tức
            ReentrantLock lockB = bidService.getLock(20L);
            boolean acquired = lockB.tryLock(100, TimeUnit.MILLISECONDS);
            if (acquired) lockB.unlock();

            done.countDown();
            t.join(1000);

            assertThat(acquired)
                    .as("Lock phiên 20 phải acquire được trong khi lock phiên 10 đang bận")
                    .isTrue();
        }

        @Test
        @DisplayName("Concurrent bids cùng phiên → chỉ một thread thực thi tại một thời điểm")
        void placeBid_concurrentSameAuction_serialized() throws InterruptedException {
            int threads = 5;
            AtomicInteger concurrentCount = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(threads);

            // Mock: mỗi bid thành công
            BigDecimal amount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(amount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername(anyString())).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(any())).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willAnswer(inv -> {
                // Kiểm tra không có hai thread cùng vào vùng này
                int current = concurrentCount.incrementAndGet();
                maxConcurrent.accumulateAndGet(current, Math::max);
                Thread.sleep(10); // mô phỏng I/O
                concurrentCount.decrementAndGet();
                successCount.incrementAndGet();
                return savedBid;
            });
            given(bidRepo.countByAuctionItem(any())).willReturn(1L);

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                final String user = "bidder0" + i;
                pool.submit(() -> {
                    try {
                        start.await();
                        bidService.placeBid(10L,
                                new Dto.PlaceBidRequest(amount), user);
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(maxConcurrent.get())
                    .as("Tối đa 1 thread trong critical section tại một thời điểm")
                    .isLessThanOrEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERIES & BROADCAST
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getBidHistory() & getMyBids()")
    class Queries {

        @Test
        @DisplayName("getBidHistory() → trả về danh sách bid theo thứ tự")
        void getBidHistory_returnsList() {
            Bid bid1 = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(new BigDecimal("10500000")).build();
            Bid bid2 = Bid.builder().id(2L).auctionItem(activeAuction)
                    .bidder(previousBidder).amount(new BigDecimal("11000000")).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(bidRepo.findBidHistoryByAuction(10L)).willReturn(List.of(bid2, bid1));

            List<Dto.BidResponse> result = bidService.getBidHistory(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).amount()).isEqualByComparingTo("11000000");
        }

        @Test
        @DisplayName("getMyBids() → trả về bid của user")
        void getMyBids_returnsUserBids() {
            Bid myBid = Bid.builder().id(5L).auctionItem(activeAuction)
                    .bidder(bidder).amount(new BigDecimal("10500000")).build();

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findByBidderOrderByBidTimeDesc(bidder)).willReturn(List.of(myBid));

            List<Dto.BidResponse> result = bidService.getMyBids("bidder01");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).bidder().username()).isEqualTo("bidder01");
        }
    }

    @Nested
    @DisplayName("broadcast methods")
    class Broadcast {

        @Test
        @DisplayName("broadcastBidUpdate() → gửi đến đúng topic")
        void broadcastBidUpdate_sendsToCorrectTopic() {
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(5L);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());

            bidService.broadcastBidUpdate(activeAuction);

            then(messagingTemplate).should()
                    .convertAndSend(eq("/topic/auction/10"), any(Dto.BidUpdateMessage.class));
        }

        @Test
        @DisplayName("broadcastAuctionClosed() → gửi message type AUCTION_CLOSED")
        void broadcastAuctionClosed_sendsClosedMessage() {
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(3L);

            bidService.broadcastAuctionClosed(activeAuction);

            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auction/10"),
                    argThat((Dto.BidUpdateMessage m) -> "AUCTION_CLOSED".equals(m.type())));
        }

        @Test
        @DisplayName("broadcastAuctionStarted() → gửi lên /topic/auctions")
        void broadcastAuctionStarted_sendsToGlobalTopic() {
            bidService.broadcastAuctionStarted(activeAuction);

            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auctions"),
                    argThat((Dto.BidUpdateMessage m) -> "AUCTION_STARTED".equals(m.type())));
        }
    }
}
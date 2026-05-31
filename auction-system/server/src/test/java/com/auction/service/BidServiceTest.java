package com.auction.service;

import com.auction.dao.BidRepository;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionItem;
import com.auction.model.Bid;
import com.auction.model.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.Role;
import com.auction.util.Dto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
        @DisplayName("Số dư không đủ → ném InsufficientBalanceException, không lưu bid")
        void placeBid_insufficientBalance_throwsCorrectException_andDoesNotSave() {
            BigDecimal bidAmount = new BigDecimal("10500000");
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            willThrow(new InsufficientBalanceException("Số dư không đủ."))
                    .given(userService).subtractBalance("bidder01", bidAmount);

            // Phải ném đúng InsufficientBalanceException (không phải RuntimeException chung)
            assertThatThrownBy(() -> bidService.doPlaceBid(10L, bidAmount, "bidder01", false))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Số dư không đủ");

            // Không được lưu bid khi trừ tiền thất bại
            then(bidRepo).should(never()).save(any());
            // Không được hoàn tiền cho ai khi chưa trừ tiền thành công
            then(userService).should(never()).addBalance(anyString(), any());
        }

        @Test
        @DisplayName("Exception giữa chừng → không broadcast, không trigger auto-bid")
        void placeBid_exceptionMidway_noBroadcastNoAutoBid() {
            BigDecimal bidAmount = new BigDecimal("10500000");
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            willThrow(new InsufficientBalanceException("Số dư không đủ."))
                    .given(userService).subtractBalance(anyString(), any());

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, bidAmount, "bidder01", false))
                    .isInstanceOf(InsufficientBalanceException.class);

            then(messagingTemplate).should(never()).convertAndSend(anyString(), any(Object.class));
            then(autoBidService).should(never()).triggerAutoBids(any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ANTI-SNIPING
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Anti-sniping extension")
    class AntiSniping {

        @Test
        @DisplayName("Bid trong 2 phút cuối → endTime gia hạn thêm 2 phút")
        void placeBid_last5Minutes_extendsEndTime() {
            LocalDateTime nearEnd = LocalDateTime.now().plusMinutes(2);
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
            assertThat(activeAuction.getSnipExtensionCount()).isEqualTo(1);
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

        @Test
        @DisplayName("Đã gia hạn 3 lần → hard close 30 giây, không gia hạn thêm")
        void placeBid_snipQuotaExhausted_hardClose() {
            LocalDateTime nearEnd = LocalDateTime.now().plusMinutes(1);
            activeAuction.setEndTime(nearEnd);
            activeAuction.setSnipExtensionCount(3); // đã hết quota

            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            bidService.doPlaceBid(10L, bidAmount, "bidder01", false);

            // Thời gian kết thúc phải ngắn hơn endTime ban đầu (hard close)
            assertThat(activeAuction.getEndTime())
                    .as("Hard close phải sớm hơn endTime ban đầu")
                    .isBefore(nearEnd);
            // Và không được vượt quá 30 giây kể từ bây giờ
            assertThat(activeAuction.getEndTime())
                    .as("Hard close không quá 30 giây kể từ hiện tại")
                    .isBefore(LocalDateTime.now().plusSeconds(31));
        }

        @Test
        @DisplayName("Gia hạn lần 1 → snipExtensionCount tăng lên 1")
        void placeBid_firstSnip_counterIncremented() {
            activeAuction.setEndTime(LocalDateTime.now().plusMinutes(2));
            activeAuction.setSnipExtensionCount(0);

            BigDecimal bidAmount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(bidAmount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(activeAuction)).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willReturn(savedBid);
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(1L);

            bidService.doPlaceBid(10L, bidAmount, "bidder01", false);

            assertThat(activeAuction.getSnipExtensionCount()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CONCURRENCY — Per-auction locking
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Concurrency — per-auction locking")
    class ConcurrencyTests {

        @Test
        @DisplayName("getLock() trả về cùng lock instance cho cùng auctionId")
        void getLock_sameId_returnsSameInstance() {
            assertThat(bidService.getLock(10L)).isSameAs(bidService.getLock(10L));
        }

        @Test
        @DisplayName("getLock() trả về lock khác nhau cho auctionId khác nhau")
        void getLock_differentIds_returnsDifferentInstances() {
            assertThat(bidService.getLock(10L)).isNotSameAs(bidService.getLock(20L));
        }

        @Test
        @DisplayName("Lock là fair ReentrantLock — tránh starvation")
        void getLock_isFair() {
            assertThat(bidService.getLock(99L).isFair()).isTrue();
        }

        @Test
        @DisplayName("Hai phiên khác nhau không block lẫn nhau — xử lý song song")
        void locks_differentAuctions_independent() throws InterruptedException {
            ReentrantLock lockA = bidService.getLock(10L);
            CountDownLatch held = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);

            // Thread giữ lock phiên 10
            Thread holder = new Thread(() -> {
                lockA.lock();
                held.countDown();
                try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                lockA.unlock();
            });
            holder.start();
            held.await(1, TimeUnit.SECONDS);

            // Lock phiên 20 phải acquire được NGAY lập tức (không bị block bởi phiên 10)
            ReentrantLock lockB = bidService.getLock(20L);
            boolean acquired = lockB.tryLock(100, TimeUnit.MILLISECONDS);
            if (acquired) lockB.unlock();
            release.countDown();
            holder.join(1000);

            assertThat(acquired)
                    .as("Lock phiên 20 phải độc lập với lock phiên 10")
                    .isTrue();
        }

        @Test
        @DisplayName("Concurrent bids cùng phiên — tối đa 1 thread trong critical section")
        void concurrentBids_sameAuction_noParallelExecution() throws InterruptedException {
            int numThreads = 8;
            AtomicInteger concurrentInSection = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            CountDownLatch allReady = new CountDownLatch(numThreads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(numThreads);

            BigDecimal amount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(amount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername(anyString())).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(any())).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willAnswer(inv -> {
                // Đếm số thread đồng thời trong critical section
                int current = concurrentInSection.incrementAndGet();
                maxConcurrent.accumulateAndGet(current, Math::max);
                Thread.sleep(5); // mô phỏng I/O nhỏ
                concurrentInSection.decrementAndGet();
                return savedBid;
            });
            given(bidRepo.countByAuctionItem(any())).willReturn(1L);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            for (int i = 0; i < numThreads; i++) {
                final String username = "bidder" + i;
                pool.submit(() -> {
                    allReady.countDown();
                    try {
                        start.await(); // tất cả thread sẵn sàng rồi mới bắt đầu cùng lúc
                        bidService.placeBid(10L, new Dto.PlaceBidRequest(amount), username);
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            allReady.await(2, TimeUnit.SECONDS);
            start.countDown(); // bắn súng — tất cả đồng loạt
            finish.await(10, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(maxConcurrent.get())
                    .as("Không được có 2 thread đồng thời trong critical section của cùng 1 phiên")
                    .isLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Concurrent bids trên 2 phiên khác nhau — chạy song song được")
        void concurrentBids_differentAuctions_runInParallel() throws InterruptedException {
            // 2 phiên khác nhau — lock của chúng độc lập → phải chạy song song
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            AtomicInteger current = new AtomicInteger(0);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(2);

            AuctionItem auction10 = activeAuction; // id=10
            AuctionItem auction20 = AuctionItem.builder()
                    .id(20L).seller(seller).name("Đồng hồ khác")
                    .startPrice(new BigDecimal("5000000"))
                    .currentPrice(new BigDecimal("5000000"))
                    .minIncrement(new BigDecimal("100000"))
                    .startTime(LocalDateTime.now().minusHours(1))
                    .endTime(LocalDateTime.now().plusHours(2))
                    .status(AuctionStatus.RUNNING).build();

            BigDecimal amount10 = new BigDecimal("10500000");
            BigDecimal amount20 = new BigDecimal("5100000");
            Bid bid10 = Bid.builder().id(1L).auctionItem(auction10).bidder(bidder).amount(amount10).build();
            Bid bid20 = Bid.builder().id(2L).auctionItem(auction20).bidder(bidder).amount(amount20).build();

            given(auctionService.findById(10L)).willReturn(auction10);
            given(auctionService.findById(20L)).willReturn(auction20);
            given(userService.findByUsername(anyString())).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(any())).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willAnswer(inv -> {
                int c = current.incrementAndGet();
                maxConcurrent.accumulateAndGet(c, Math::max);
                Thread.sleep(50); // I/O dài hơn để hai thread thực sự overlap
                current.decrementAndGet();
                Bid b = inv.getArgument(0);
                return b.getAuctionItem().getId().equals(10L) ? bid10 : bid20;
            });
            given(bidRepo.countByAuctionItem(any())).willReturn(1L);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            pool.submit(() -> {
                try { start.await(); bidService.placeBid(10L, new Dto.PlaceBidRequest(amount10), "bidder01"); }
                catch (Exception ignored) { } finally { finish.countDown(); }
            });
            pool.submit(() -> {
                try { start.await(); bidService.placeBid(20L, new Dto.PlaceBidRequest(amount20), "bidder02"); }
                catch (Exception ignored) { } finally { finish.countDown(); }
            });

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(maxConcurrent.get())
                    .as("Hai phiên khác nhau phải chạy song song (maxConcurrent = 2)")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Lock không bị rò rỉ khi bid ném exception — thread sau vẫn acquire được")
        void lockNotLeaked_whenExceptionThrown() throws InterruptedException {
            BigDecimal amount = new BigDecimal("10500000");

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(any())).willReturn(Optional.empty());
            willThrow(new InsufficientBalanceException("Số dư không đủ."))
                    .given(userService).subtractBalance(anyString(), any());

            // Thread 1: bid thất bại (ném exception)
            assertThatThrownBy(() ->
                bidService.doPlaceBid(10L, amount, "bidder01", false)
            ).isInstanceOf(InsufficientBalanceException.class);

            // Sau exception, lock phải đã được unlock — thread 2 acquire được
            ReentrantLock lock = bidService.getLock(10L);
            boolean canAcquire = lock.tryLock(200, TimeUnit.MILLISECONDS);
            if (canAcquire) lock.unlock();

            assertThat(canAcquire)
                    .as("Lock phải được release ngay cả khi có exception (finally block)")
                    .isTrue();
            assertThat(lock.isLocked())
                    .as("Lock không được còn bị giữ sau exception")
                    .isFalse();
        }

        @Test
        @DisplayName("Lost update prevention — lock đảm bảo các bid được serialize")
        void noLostUpdate_bidsAreSerializedByLock() throws InterruptedException {
            // Mục tiêu: kiểm tra rằng per-auction lock serialize tất cả bid vào cùng
            // một phiên, không có hai thread nào đồng thời ở trong critical section.
            //
            // Mock không mô phỏng việc tăng giá thực (đó là trách nhiệm của integration test).
            // Unit test này chỉ kiểm tra: khi 5 thread bid đồng loạt, maxConcurrent <= 1.
            int numThreads = 5;
            AtomicInteger concurrentInSection = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            AtomicInteger savedCount = new AtomicInteger(0);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(numThreads);

            BigDecimal amount = new BigDecimal("10500000");
            Bid savedBid = Bid.builder().id(1L).auctionItem(activeAuction)
                    .bidder(bidder).amount(amount).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername(anyString())).willReturn(bidder);
            given(bidRepo.findTopBidByAuction(any())).willReturn(Optional.empty());
            given(bidRepo.save(any(Bid.class))).willAnswer(inv -> {
                int c = concurrentInSection.incrementAndGet();
                maxConcurrent.accumulateAndGet(c, Math::max);
                Thread.sleep(5);
                concurrentInSection.decrementAndGet();
                savedCount.incrementAndGet();
                return savedBid;
            });
            given(bidRepo.countByAuctionItem(any())).willReturn(1L);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        bidService.placeBid(10L, new Dto.PlaceBidRequest(amount), "bidder01");
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            // maxConcurrent <= 1: lock đảm bảo serialize — đây là điều quan trọng
            assertThat(maxConcurrent.get())
                    .as("Lock phải đảm bảo tối đa 1 thread trong critical section — không lost update")
                    .isLessThanOrEqualTo(1);

            // savedCount == 1: đúng về nghiệp vụ — sau thread 1 thành công,
            // currentPrice tăng lên, các thread sau ném InvalidBidException vì
            // amount không còn đủ minimum. Đây chính là hành vi chống lost update.
            assertThat(savedCount.get())
                    .as("Chỉ 1 bid được lưu — các bid sau bị reject vì currentPrice đã tăng")
                    .isEqualTo(1);
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

    // ─────────────────────────────────────────────────────────────────
    // BROADCAST
    // ─────────────────────────────────────────────────────────────────
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
        @DisplayName("broadcastAuctionClosed() → gửi message type AUCTION_CLOSED đến cả hai topic")
        void broadcastAuctionClosed_sendsToBothTopics() {
            given(bidRepo.countByAuctionItem(activeAuction)).willReturn(3L);

            bidService.broadcastAuctionClosed(activeAuction);

            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auction/10"),
                    argThat((Dto.BidUpdateMessage m) -> "AUCTION_CLOSED".equals(m.type())));
            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auctions"),
                    argThat((Dto.BidUpdateMessage m) -> "AUCTION_CLOSED".equals(m.type())));
        }

        @Test
        @DisplayName("broadcastAuctionStarted() → gửi lên /topic/auctions với type AUCTION_STARTED")
        void broadcastAuctionStarted_sendsCorrectTypeToGlobalTopic() {
            bidService.broadcastAuctionStarted(activeAuction);

            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auctions"),
                    argThat((Dto.BidUpdateMessage m) -> "AUCTION_STARTED".equals(m.type())));
        }

        @Test
        @DisplayName("broadcastAuctionCreated() → gửi type AUCTION_CREATED kèm sellerId")
        void broadcastAuctionCreated_includesSeller() {
            bidService.broadcastAuctionCreated(activeAuction);

            then(messagingTemplate).should().convertAndSend(
                    eq("/topic/auctions"),
                    argThat((Dto.BidUpdateMessage m) ->
                            "AUCTION_CREATED".equals(m.type()) &&
                            "seller01".equals(m.leaderUsername())));
        }
    }
}
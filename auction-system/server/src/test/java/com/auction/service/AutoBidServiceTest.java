package com.auction.service;

import com.auction.dao.AutoBidConfigRepository;
import com.auction.model.AuctionItem;
import com.auction.model.AutoBidConfig;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoBidService Tests")
class AutoBidServiceTest {

    @Mock AutoBidConfigRepository configRepo;
    @Mock UserService userService;
    @Mock AuctionService auctionService;
    @Mock BidService bidService;

    @InjectMocks AutoBidService autoBidService;

    private User seller;
    private User bidder;
    private User bidder2;
    private AuctionItem activeAuction;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(99L).username("seller01")
                .email("s@test.com").role(Role.SELLER).balance(BigDecimal.ZERO).build();

        bidder = User.builder().id(1L).username("bidder01")
                .email("b1@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("10000000")).build();

        bidder2 = User.builder().id(2L).username("bidder02")
                .email("b2@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("10000000")).build();

        activeAuction = AuctionItem.builder()
                .id(10L)
                .seller(seller)
                .name("Tranh sơn mài")
                .startPrice(new BigDecimal("2000000"))
                .currentPrice(new BigDecimal("2000000"))
                .minIncrement(new BigDecimal("100000"))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.RUNNING)
                .build();

        // lenient: stub này chỉ cần trong test đi qua triggerAutoBids().
        // Dùng lenient() để Mockito không báo UnnecessaryStubbing ở test không dùng tới.
        lenient().when(bidService.getLock(anyLong())).thenReturn(new ReentrantLock(true));
    }

    // ─────────────────────────────────────────────────────────────────
    // SETUP AUTO BID
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("setupAutoBid()")
    class SetupAutoBid {

        @Test
        @DisplayName("Cấu hình mới hợp lệ → lưu config và kích hoạt trigger")
        void setup_validNewConfig_saved() {
            Dto.AutoBidRequest req = new Dto.AutoBidRequest(
                    new BigDecimal("5000000"), new BigDecimal("200000"));
            AutoBidConfig savedConfig = AutoBidConfig.builder()
                    .id(1L).bidder(bidder).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findByBidderAndAuctionItem(bidder, activeAuction))
                    .willReturn(Optional.empty());
            given(configRepo.save(any(AutoBidConfig.class))).willReturn(savedConfig);
            // triggerAutoBids bên trong setupAutoBid: auctionService cần trả về fresh item
            given(configRepo.findActiveConfigsExcluding(any(), any()))
                    .willReturn(Collections.emptyList());

            AutoBidConfig result = autoBidService.setupAutoBid(10L, req, "bidder01");

            assertThat(result.getMaxAmount()).isEqualByComparingTo("5000000");
            assertThat(result.isActive()).isTrue();
            then(configRepo).should().save(any(AutoBidConfig.class));
        }

        @Test
        @DisplayName("Đã có config trước → cập nhật config cũ, không tạo mới")
        void setup_existingConfig_updatesInPlace() {
            AutoBidConfig existingConfig = AutoBidConfig.builder()
                    .id(1L).bidder(bidder).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("3000000"))
                    .increment(new BigDecimal("100000"))
                    .isActive(true).build();

            Dto.AutoBidRequest req = new Dto.AutoBidRequest(
                    new BigDecimal("6000000"), new BigDecimal("300000"));

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findByBidderAndAuctionItem(bidder, activeAuction))
                    .willReturn(Optional.of(existingConfig));
            given(configRepo.save(any(AutoBidConfig.class))).willReturn(existingConfig);
            given(configRepo.findActiveConfigsExcluding(any(), any()))
                    .willReturn(Collections.emptyList());

            autoBidService.setupAutoBid(10L, req, "bidder01");

            assertThat(existingConfig.getMaxAmount()).isEqualByComparingTo("6000000");
            assertThat(existingConfig.getIncrement()).isEqualByComparingTo("300000");
            // Chỉ save một lần (upsert, không insert thêm)
            then(configRepo).should(times(1)).save(existingConfig);
        }

        @Test
        @DisplayName("maxAmount <= currentPrice → ném IllegalArgumentException")
        void setup_maxAmountTooLow_throws() {
            Dto.AutoBidRequest req = new Dto.AutoBidRequest(
                    new BigDecimal("1000000"), // < currentPrice 2M
                    new BigDecimal("100000"));

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);

            assertThatThrownBy(() -> autoBidService.setupAutoBid(10L, req, "bidder01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lớn hơn giá hiện tại");
        }

        @Test
        @DisplayName("maxAmount = currentPrice → ném IllegalArgumentException")
        void setup_maxAmountEqualCurrentPrice_throws() {
            Dto.AutoBidRequest req = new Dto.AutoBidRequest(
                    new BigDecimal("2000000"), // = currentPrice
                    new BigDecimal("100000"));

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);

            assertThatThrownBy(() -> autoBidService.setupAutoBid(10L, req, "bidder01"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Phiên không còn nhận bid → ném IllegalStateException")
        void setup_auctionNotAcceptingBids_throws() {
            activeAuction.setStatus(AuctionStatus.FINISHED);
            Dto.AutoBidRequest req = new Dto.AutoBidRequest(
                    new BigDecimal("5000000"), new BigDecimal("100000"));

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);

            assertThatThrownBy(() -> autoBidService.setupAutoBid(10L, req, "bidder01"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("không đang chạy");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL AUTO BID
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancelAutoBid()")
    class CancelAutoBid {

        @Test
        @DisplayName("Có config → đặt isActive = false")
        void cancel_existingConfig_deactivates() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findByBidderAndAuctionItem(bidder, activeAuction))
                    .willReturn(Optional.of(config));
            given(configRepo.save(any())).willReturn(config);

            autoBidService.cancelAutoBid(10L, "bidder01");

            assertThat(config.isActive()).isFalse();
            then(configRepo).should().save(config);
        }

        @Test
        @DisplayName("Không có config → không làm gì (no exception)")
        void cancel_noExistingConfig_noOp() {
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findByBidderAndAuctionItem(bidder, activeAuction))
                    .willReturn(Optional.empty());

            assertThatCode(() -> autoBidService.cancelAutoBid(10L, "bidder01"))
                    .doesNotThrowAnyException();

            then(configRepo).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TRIGGER AUTO BIDS
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("triggerAutoBids()")
    class TriggerAutoBids {


        @Test
        @DisplayName("Phiên không nhận bid → không query config, không đặt giá")
        void trigger_auctionClosed_noAction() {
            activeAuction.setStatus(AuctionStatus.FINISHED);

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(configRepo).should(never()).findActiveConfigsExcluding(any(), any());
            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Phiên hết giờ dù RUNNING → không kích hoạt")
        void trigger_expiredAuction_noAction() {
            activeAuction.setEndTime(LocalDateTime.now().minusSeconds(1));
            // isAcceptingBids() trả false ngay ở guard đầu tiên → không vào lock, không gọi findById

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Không có auto-bid config nào → không đặt giá")
        void trigger_noConfigs_noAction() {
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(Collections.emptyList());

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Có config hợp lệ → getLock() được gọi trước khi doPlaceBid()")
        void trigger_validConfig_acquiresLockBeforeBid() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));

            autoBidService.triggerAutoBids(activeAuction, bidder);

            // getLock() phải được gọi với đúng auctionId
            then(bidService).should().getLock(10L);
            then(bidService).should().doPlaceBid(eq(10L), any(), any(), eq(true));
        }

        @Test
        @DisplayName("Có config hợp lệ, nextBid trong ngưỡng → tự động đặt giá đúng amount")
        void trigger_validConfig_placesCorrectBidAmount() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            // currentPrice = 2M, increment = 200K → nextBid = 2.2M < maxAmount 5M
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(bidService).should().doPlaceBid(
                    eq(10L),
                    eq(new BigDecimal("2200000")),
                    eq("bidder02"),
                    eq(true));
        }

        @Test
        @DisplayName("nextBid vượt maxAmount → config bị vô hiệu hóa, không đặt giá")
        void trigger_nextBidExceedsMax_deactivatesConfig() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("2050000")) // 2M + 200K = 2.2M > 2.05M
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));
            given(configRepo.save(config)).willReturn(config);

            autoBidService.triggerAutoBids(activeAuction, bidder);

            assertThat(config.isActive()).isFalse();
            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("nextBid = maxAmount chính xác → chấp nhận và đặt giá")
        void trigger_nextBidEqualsMax_placesBid() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("2200000")) // = currentPrice + increment
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(bidService).should().doPlaceBid(
                    eq(10L),
                    eq(new BigDecimal("2200000")),
                    eq("bidder02"),
                    eq(true));
        }

        @Test
        @DisplayName("leader null → resolve từ bid history rồi loại ra khỏi trigger")
        void trigger_nullLeader_resolvesFromHistory() {
            Dto.UserResponse bidderDto = new Dto.UserResponse(
                    1L, "bidder01", "bidder01@test.com",
                    Role.BIDDER, new BigDecimal("10000000"), true, LocalDateTime.now());
            Dto.BidResponse lastBid = new Dto.BidResponse(
                    1L, 10L, "Tranh sơn mài", bidderDto,
                    new BigDecimal("2000000"), false, LocalDateTime.now());

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(bidService.getBidHistory(10L)).willReturn(List.of(lastBid));
            given(userService.findByUsername("bidder01")).willReturn(bidder);
            given(configRepo.findActiveConfigsExcluding(eq(activeAuction), eq(bidder)))
                    .willReturn(Collections.emptyList());

            autoBidService.triggerAutoBids(activeAuction, null);

            // Verify loại đúng leader
            then(configRepo).should().findActiveConfigsExcluding(activeAuction, bidder);
        }

        @Test
        @DisplayName("leader null, không có bid history → leader = null, query không loại ai")
        void trigger_nullLeader_noBidHistory_queriesWithNullLeader() {
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(bidService.getBidHistory(10L)).willReturn(Collections.emptyList());
            given(configRepo.findActiveConfigsExcluding(eq(activeAuction), isNull()))
                    .willReturn(Collections.emptyList());

            assertThatCode(() -> autoBidService.triggerAutoBids(activeAuction, null))
                    .doesNotThrowAnyException();

            then(configRepo).should().findActiveConfigsExcluding(activeAuction, null);
        }

        @Test
        @DisplayName("PriorityQueue: Người có maxAmount cao nhất đấu giá (Proxy Bidding)")
        void trigger_multipleConfigs_usesHighestMaxAmountAndProxyBidding() {
            // RunnerUp: bidder01 max = 3,000,000
            AutoBidConfig runnerUpConfig = AutoBidConfig.builder()
                    .id(1L).bidder(bidder).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("3000000"))
                    .increment(new BigDecimal("100000"))
                    .isActive(true).build();

            // Winner: bidder02 max = 5,000,000
            AutoBidConfig winnerConfig = AutoBidConfig.builder()
                    .id(2L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000")) // increment của winner là 200k
                    .isActive(true).build();

            User currentLeader = User.builder().id(50L).username("someone_else")
                    .role(Role.BIDDER).balance(BigDecimal.ZERO).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            
            // Giả lập Repository trả về cả 2 (không quan tâm thứ tự vì sẽ vào PriorityQueue)
            given(configRepo.findActiveConfigsExcluding(activeAuction, currentLeader))
                    .willReturn(List.of(runnerUpConfig, winnerConfig));

            autoBidService.triggerAutoBids(activeAuction, currentLeader);

            // Winner (bidder02) sẽ đặt giá bằng runnerUp.maxAmount (3,000,000) + winner.increment (200,000)
            // nextBid = 3,200,000
            then(bidService).should().doPlaceBid(
                    eq(10L), eq(new BigDecimal("3200000")), eq("bidder02"), eq(true));
            
            // RunnerUp (bidder01) không được đặt giá
            then(bidService).should(never()).doPlaceBid(
                    eq(10L), any(), eq("bidder01"), eq(true));
        }

        @Test
        @DisplayName("doPlaceBid ném exception → lock vẫn được nhả (no deadlock)")
        void trigger_bidServiceThrows_lockAlwaysReleased() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));

            ReentrantLock realLock = new ReentrantLock(true);
            given(bidService.getLock(10L)).willReturn(realLock);
            willThrow(new RuntimeException("DB lỗi"))
                    .given(bidService).doPlaceBid(any(), any(), any(), anyBoolean());

            // Exception từ doPlaceBid phải propagate ra
            assertThatThrownBy(() -> autoBidService.triggerAutoBids(activeAuction, bidder))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB lỗi");

            // Lock phải được nhả (không bị giữ lại) → tryLock() phải thành công ngay
            assertThat(realLock.isLocked())
                    .as("Lock phải được nhả trong finally dù doPlaceBid ném exception")
                    .isFalse();
        }
    }
}
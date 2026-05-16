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

    private User bidder;
    private User bidder2;
    private AuctionItem activeAuction;

    @BeforeEach
    void setUp() {
        bidder = User.builder().id(1L).username("bidder01")
                .email("b1@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("10000000")).build();

        bidder2 = User.builder().id(2L).username("bidder02")
                .email("b2@test.com").role(Role.BIDDER)
                .balance(new BigDecimal("10000000")).build();

        activeAuction = AuctionItem.builder()
                .id(10L)
                .seller(User.builder().id(99L).username("seller01").email("s@test.com")
                        .role(Role.SELLER).balance(BigDecimal.ZERO).build())
                .name("Tranh sơn mài")
                .startPrice(new BigDecimal("2000000"))
                .currentPrice(new BigDecimal("2000000"))
                .minIncrement(new BigDecimal("100000"))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.RUNNING)
                .build();
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
        @DisplayName("Phiên không nhận bid → không kích hoạt")
        void trigger_auctionClosed_noAction() {
            activeAuction.setStatus(AuctionStatus.FINISHED);

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(configRepo).should(never()).findActiveConfigsExcluding(any(), any());
        }

        @Test
        @DisplayName("Không có auto-bid config nào → không đặt giá")
        void trigger_noConfigs_noAction() {
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(Collections.emptyList());

            autoBidService.triggerAutoBids(activeAuction, bidder);

            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Có config, nextBid trong ngưỡng → tự động đặt giá")
        void trigger_validConfig_placesBid() {
            AutoBidConfig config = AutoBidConfig.builder()
                    .id(1L).bidder(bidder2).auctionItem(activeAuction)
                    .maxAmount(new BigDecimal("5000000"))
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            // nextBid = 2M + 200K = 2.2M < maxAmount 5M → hợp lệ
            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));
            given(auctionService.findById(10L)).willReturn(activeAuction);

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
                    .maxAmount(new BigDecimal("2050000")) // nextBid = 2.2M > 2.05M → vượt ngưỡng
                    .increment(new BigDecimal("200000"))
                    .isActive(true).build();

            given(configRepo.findActiveConfigsExcluding(activeAuction, bidder))
                    .willReturn(List.of(config));
            given(configRepo.save(config)).willReturn(config);

            autoBidService.triggerAutoBids(activeAuction, bidder);

            assertThat(config.isActive()).isFalse();
            then(bidService).should(never()).doPlaceBid(any(), any(), any(), anyBoolean());
        }
    }
}
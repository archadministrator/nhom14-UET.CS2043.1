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
            BigDecimal bidAmount = new BigDecimal("10500000"); // currentPrice + minIncrement
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
            BigDecimal exactMin = new BigDecimal("10500000"); // 10M + 500K
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
            BigDecimal tooLow = new BigDecimal("10000001"); // < 10M + 500K
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, tooLow, "bidder01", false))
                    .isInstanceOf(InvalidBidException.class)
                    .hasMessageContaining("tối thiểu");
        }

        @Test
        @DisplayName("Giá đặt bằng currentPrice (thiếu increment) → ném InvalidBidException")
        void placeBid_exactCurrentPrice_throws() {
            BigDecimal currentPrice = new BigDecimal("10000000"); // không có increment
            given(auctionService.findById(10L)).willReturn(activeAuction);
            given(userService.findByUsername("bidder01")).willReturn(bidder);

            assertThatThrownBy(() -> bidService.doPlaceBid(10L, currentPrice, "bidder01", false))
                    .isInstanceOf(InvalidBidException.class);
        }

        @Test
        @DisplayName("Số dư không đủ → ném RuntimeException, không lưu bid")
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
            // Đặt endTime còn 3 phút nữa → kích hoạt anti-sniping
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

            // endTime phải lớn hơn nearEnd (đã được gia hạn)
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
    // GET BID HISTORY / MY BIDS
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
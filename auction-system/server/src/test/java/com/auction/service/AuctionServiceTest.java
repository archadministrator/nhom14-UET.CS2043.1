package com.auction.service;

import com.auction.dao.AuctionItemRepository;
import com.auction.dao.BidRepository;
import com.auction.exception.AccessDeniedException;
import com.auction.exception.AuctionNotFoundException;
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
@DisplayName("AuctionService Tests")
class AuctionServiceTest {

    @Mock AuctionItemRepository auctionRepo;
    @Mock BidRepository bidRepo;
    @Mock UserService userService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks AuctionService auctionService;

    private User seller;
    private User admin;
    private AuctionItem runningAuction;
    private AuctionItem openAuction;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(1L).username("seller01")
                .email("s@test.com").role(Role.SELLER).balance(BigDecimal.ZERO).build();

        admin = User.builder().id(2L).username("admin01")
                .email("a@test.com").role(Role.ADMIN).balance(BigDecimal.ZERO).build();

        openAuction = AuctionItem.builder()
                .id(10L)
                .seller(seller)
                .name("Laptop Gaming")
                .description("Mô tả")
                .startPrice(new BigDecimal("5000000"))
                .currentPrice(new BigDecimal("5000000"))
                .minIncrement(new BigDecimal("100000"))
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .status(AuctionStatus.OPEN)
                .build();

        runningAuction = AuctionItem.builder()
                .id(11L)
                .seller(seller)
                .name("Đồng hồ cổ")
                .description("Cổ vật")
                .startPrice(new BigDecimal("1000000"))
                .currentPrice(new BigDecimal("1500000"))
                .minIncrement(new BigDecimal("50000"))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.RUNNING)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Tạo phiên hợp lệ → lưu thành công và trả về response")
        void create_valid_success() {
            LocalDateTime start = LocalDateTime.now().plusHours(1);
            LocalDateTime end = start.plusHours(2);
            Dto.CreateAuctionRequest req = new Dto.CreateAuctionRequest(
                    "Laptop", "Mô tả", new BigDecimal("5000000"),
                    new BigDecimal("100000"), start, end, null);

            given(userService.findByUsername("seller01")).willReturn(seller);
            given(auctionRepo.save(any(AuctionItem.class))).willReturn(openAuction);
            given(bidRepo.countByAuctionItem(any())).willReturn(0L);

            Dto.AuctionResponse result = auctionService.create(req, "seller01");

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Laptop Gaming");
            then(auctionRepo).should().save(any(AuctionItem.class));
        }

        @Test
        @DisplayName("endTime trước startTime → ném IllegalArgumentException")
        void create_endBeforeStart_throws() {
            LocalDateTime start = LocalDateTime.now().plusHours(2);
            LocalDateTime end = start.minusHours(1); // end < start
            Dto.CreateAuctionRequest req = new Dto.CreateAuctionRequest(
                    "Test", null, new BigDecimal("1000"),
                    new BigDecimal("1000"), start, end, null);
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> auctionService.create(req, "seller01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sau thời gian bắt đầu");
        }

        @Test
        @DisplayName("Phiên chưa đủ 5 phút → ném IllegalArgumentException")
        void create_durationLessThan5Min_throws() {
            LocalDateTime start = LocalDateTime.now().plusHours(1);
            LocalDateTime end = start.plusMinutes(3); // chỉ 3 phút
            Dto.CreateAuctionRequest req = new Dto.CreateAuctionRequest(
                    "Test", null, new BigDecimal("1000"),
                    new BigDecimal("1000"), start, end, null);
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> auctionService.create(req, "seller01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ít nhất 5 phút");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Seller sửa phiên OPEN → cập nhật thành công")
        void update_bySellerOnOpen_success() {
            given(auctionRepo.findById(10L)).willReturn(Optional.of(openAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);
            given(auctionRepo.save(any(AuctionItem.class))).willReturn(openAuction);
            given(bidRepo.countByAuctionItem(any())).willReturn(0L);

            Dto.UpdateAuctionRequest req = new Dto.UpdateAuctionRequest("Laptop Pro", null, null);
            Dto.AuctionResponse result = auctionService.update(10L, req, "seller01");

            assertThat(result).isNotNull();
            assertThat(openAuction.getName()).isEqualTo("Laptop Pro");
        }

        @Test
        @DisplayName("Admin sửa phiên OPEN → cập nhật thành công")
        void update_byAdminOnOpen_success() {
            given(auctionRepo.findById(10L)).willReturn(Optional.of(openAuction));
            given(userService.findByUsername("admin01")).willReturn(admin);
            given(auctionRepo.save(any(AuctionItem.class))).willReturn(openAuction);
            given(bidRepo.countByAuctionItem(any())).willReturn(0L);

            Dto.UpdateAuctionRequest req = new Dto.UpdateAuctionRequest(null, "Mô tả mới", null);
            auctionService.update(10L, req, "admin01");

            assertThat(openAuction.getDescription()).isEqualTo("Mô tả mới");
        }

        @Test
        @DisplayName("Người khác sửa phiên → ném AccessDeniedException")
        void update_byStranger_throws() {
            User stranger = User.builder().id(3L).username("bidder01")
                    .email("b@test.com").role(Role.BIDDER).balance(BigDecimal.ZERO).build();
            given(auctionRepo.findById(10L)).willReturn(Optional.of(openAuction));
            given(userService.findByUsername("bidder01")).willReturn(stranger);

            assertThatThrownBy(() -> auctionService.update(10L,
                    new Dto.UpdateAuctionRequest("X", null, null), "bidder01"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Sửa phiên RUNNING → ném IllegalStateException")
        void update_runningAuction_throws() {
            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> auctionService.update(11L,
                    new Dto.UpdateAuctionRequest("X", null, null), "seller01"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OPEN");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Xóa phiên OPEN → soft delete thành CANCELED")
        void delete_openAuction_softDelete() {
            given(auctionRepo.findById(10L)).willReturn(Optional.of(openAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);
            given(auctionRepo.save(any(AuctionItem.class))).willReturn(openAuction);

            auctionService.delete(10L, "seller01");

            assertThat(openAuction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        }

        @Test
        @DisplayName("Xóa phiên RUNNING → hoàn tiền cho người dẫn đầu")
        void delete_runningAuction_refundsTopBidder() {
            User topBidder = User.builder().id(4L).username("bidder99")
                    .email("b99@test.com").role(Role.BIDDER).balance(new BigDecimal("1000000")).build();
            Bid topBid = Bid.builder().id(1L).auctionItem(runningAuction)
                    .bidder(topBidder).amount(new BigDecimal("1500000")).build();

            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);
            given(bidRepo.findTopBidByAuction(runningAuction)).willReturn(Optional.of(topBid));
            given(auctionRepo.save(any())).willReturn(runningAuction);

            auctionService.delete(11L, "seller01");

            then(userService).should().addBalance("bidder99", new BigDecimal("1500000"));
            assertThat(runningAuction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        }

        @Test
        @DisplayName("Xóa phiên FINISHED → ném IllegalStateException")
        void delete_finishedAuction_throws() {
            runningAuction.setStatus(AuctionStatus.FINISHED);
            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> auctionService.delete(11L, "seller01"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("kết thúc");
        }

        @Test
        @DisplayName("Xóa phiên PAID → ném IllegalStateException")
        void delete_paidAuction_throws() {
            runningAuction.setStatus(AuctionStatus.PAID);
            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction));
            given(userService.findByUsername("seller01")).willReturn(seller);

            assertThatThrownBy(() -> auctionService.delete(11L, "seller01"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }


    @Nested
    @DisplayName("Lifecycle transitions")
    class Lifecycle {

        @Test
        @DisplayName("activateAuction() → status = RUNNING")
        void activate_setsRunning() {
            given(auctionRepo.save(openAuction)).willReturn(openAuction);

            auctionService.activateAuction(openAuction);

            assertThat(openAuction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        }

        @Test
        @DisplayName("closeAuction() không có bid → status = CANCELED")
        void close_noBids_canceled() {
            given(bidRepo.countByAuctionItem(runningAuction)).willReturn(0L);
            given(auctionRepo.save(runningAuction)).willReturn(runningAuction);

            auctionService.closeAuction(runningAuction);

            assertThat(runningAuction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        }

        @Test
        @DisplayName("closeAuction() có bid → status = FINISHED và winner được gán")
        void close_withBids_finishedAndWinnerSet() {
            User winner = User.builder().id(5L).username("winner01")
                    .email("w@test.com").role(Role.BIDDER).balance(BigDecimal.ZERO).build();
            Bid topBid = Bid.builder().id(1L).auctionItem(runningAuction)
                    .bidder(winner).amount(new BigDecimal("2000000")).build();

            given(bidRepo.countByAuctionItem(runningAuction)).willReturn(3L);
            given(bidRepo.findTopBidByAuction(runningAuction)).willReturn(Optional.of(topBid));
            given(auctionRepo.save(runningAuction)).willReturn(runningAuction);

            auctionService.closeAuction(runningAuction);

            assertThat(runningAuction.getStatus()).isEqualTo(AuctionStatus.FINISHED);
            assertThat(runningAuction.getWinner().getUsername()).isEqualTo("winner01");
        }

        @Test
        @DisplayName("markPaid() khi FINISHED → status = PAID")
        void markPaid_finished_success() {
            runningAuction.setStatus(AuctionStatus.FINISHED);
            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction));
            given(auctionRepo.save(runningAuction)).willReturn(runningAuction);
            given(bidRepo.countByAuctionItem(any())).willReturn(3L);

            Dto.AuctionResponse result = auctionService.markPaid(11L, "admin01");

            assertThat(runningAuction.getStatus()).isEqualTo(AuctionStatus.PAID);
        }

        @Test
        @DisplayName("markPaid() khi chưa FINISHED → ném IllegalStateException")
        void markPaid_notFinished_throws() {
            given(auctionRepo.findById(11L)).willReturn(Optional.of(runningAuction)); // RUNNING

            assertThatThrownBy(() -> auctionService.markPaid(11L, "admin01"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FINISHED");
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("ID tồn tại → trả về AuctionItem")
        void findById_exists_returnsItem() {
            given(auctionRepo.findById(10L)).willReturn(Optional.of(openAuction));

            AuctionItem result = auctionService.findById(10L);

            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("ID không tồn tại → ném AuctionNotFoundException")
        void findById_notFound_throws() {
            given(auctionRepo.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> auctionService.findById(999L))
                    .isInstanceOf(AuctionNotFoundException.class);
        }
    }
}
package com.auction.client.controller;

import com.auction.client.model.ClientDto.*;
import com.auction.client.realtime.AuctionEvent;
import com.auction.client.realtime.AuctionEventBus;
import com.auction.client.realtime.AuctionObserver;
import com.auction.client.service.ApiService;
import com.auction.client.service.SessionManager;
import com.auction.client.service.WebSocketService;
import com.auction.client.util.FxUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AuctionDetailController implements AuctionObserver {

    @FXML private Label lblAuctionName, lblStatusBadge, lblCountdown;
    @FXML private Label lblProductName, lblDescription;
    @FXML private Label lblStartPrice, lblMinIncrement, lblSeller, lblEndTime;
    @FXML private Label lblCurrentPrice, lblLeader, lblTotalBids;
    @FXML private VBox  bidPanel, winnerPanel;
    @FXML private TextField txtBidAmount, txtAutoMax;
    @FXML private Button    btnBid;
    @FXML private Label     lblBidMsg, lblWinner;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis, yAxis;

    @FXML private TableView<BidDto>          tblBids;
    @FXML private TableColumn<BidDto,String> colBidTime, colBidder, colBidAmount, colBidAuto;

    private AuctionDto currentAuction;

    // ── Countdown timer ───────────────────────────────────────────────────────
    // Dùng ScheduledExecutorService thay vì java.util.Timer:
    //   - java.util.Timer dùng 1 thread duy nhất cho tất cả task — nếu task trước
    //     bị block, task sau bị trễ. ScheduledExecutorService tránh vấn đề này.
    //   - Dễ shutdown hơn: shutdownNow() + isShutdown() an toàn hơn Timer.cancel().
    //   - ScheduledFuture cho phép cancel task riêng lẻ mà không hủy cả pool.
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-countdown");
                t.setDaemon(true); // tự hủy khi JVM tắt — không giữ process sống
                return t;
            });
    private ScheduledFuture<?> countdownTask;

    private final ObservableList<BidDto> bidData = FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();

    private final ApiService      api      = ApiService.getInstance();
    private final SessionManager  session  = SessionManager.getInstance();
    private final WebSocketService ws      = WebSocketService.getInstance();
    private final AuctionEventBus  eventBus = AuctionEventBus.getInstance();

    private static final NumberFormat CURRENCY =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
    }

    /**
     * Entry point khi navigate từ AuctionList sang.
     *
     * Vấn đề của cách cũ (setAuction trực tiếp từ cached object):
     *   1. Object AuctionDto được truyền vào là snapshot từ lần load AuctionList trước
     *      → currentPrice, totalBids, endTime có thể đã lỗi thời vài giây đến vài phút.
     *   2. subscribeRealtime() được gọi SAU khi fetch xong → race condition:
     *      bid đến trong khoảng thời gian fetch bị bỏ lỡ hoàn toàn.
     *
     * Luồng đúng — Subscribe trước, fetch sau, merge:
     *   Bước 1: render UI ngay với dữ liệu cũ (UX không bị trắng màn hình)
     *   Bước 2: subscribe realtime NGAY LẬP TỨC → không bỏ lỡ event nào
     *   Bước 3: fetch dữ liệu mới nhất từ server song song
     *   Bước 4: fetch bid history song song
     *   Bước 5: merge — áp dữ liệu mới lên UI, bảo toàn event realtime đã nhận
     *            trong bước 2-3 (chúng đã update currentAuction trực tiếp)
     */
    public void setAuction(AuctionDto auction) {
        this.currentAuction = auction;

        Platform.runLater(() -> {
            // Bước 1: render UI ngay với dữ liệu tạm — user thấy màn hình ngay
            setupBidTable();
            setupChart();
            populateInfo();
            if (session.isSeller()) hideBidHistorySection();

            // Bước 2: subscribe TRƯỚC KHI fetch — không bỏ lỡ event nào
            subscribeRealtime();

            // Bước 3 + 4: fetch fresh data và bid history song song
            fetchFreshDataAndHistory();
        });
    }

    /**
     * Fetch dữ liệu mới nhất từ server trên background thread.
     *
     * Dùng hai thread song song để giảm tổng thời gian chờ:
     *   Thread A: GET /auctions/{id}     → dữ liệu phiên mới nhất
     *   Thread B: GET /bids/auction/{id} → lịch sử bid
     *
     * Sau khi cả hai hoàn thành, merge vào UI trên FX thread.
     * Nếu có realtime event đến TRONG LÚC đang fetch: chúng đã update
     * currentAuction.currentPrice trực tiếp (qua handleNewBidEvent),
     * nên khi mergeWithFreshData() chạy, nó so sánh và giữ giá cao hơn.
     */
    private void fetchFreshDataAndHistory() {
        final long auctionId = currentAuction.getId();

        // Dùng mảng để share kết quả giữa hai thread vào FX thread
        final AuctionDto[] freshRef   = new AuctionDto[1];
        final List<BidDto>[] histRef  = new List[1];
        final String[] errorRef       = new String[1];

        Thread fetchAuction = new Thread(() -> {
            try {
                freshRef[0] = api.getAuction(auctionId);
            } catch (Exception e) {
                errorRef[0] = "Không tải được dữ liệu phiên: " + e.getMessage();
            }
        }, "fetch-auction-" + auctionId);

        Thread fetchHistory = new Thread(() -> {
            try {
                histRef[0] = api.getBidHistory(auctionId);
            } catch (Exception e) {
                // Lịch sử không load được chỉ là non-critical
                histRef[0] = java.util.Collections.emptyList();
            }
        }, "fetch-history-" + auctionId);

        fetchAuction.setDaemon(true);
        fetchHistory.setDaemon(true);
        fetchAuction.start();
        fetchHistory.start();

        // Thread gom kết quả — chờ cả hai xong rồi update UI
        Thread merger = new Thread(() -> {
            try {
                fetchAuction.join(5000); // timeout 5s
                fetchHistory.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(() -> {
                if (errorRef[0] != null) {
                    showBidMsg(errorRef[0], false);
                } else if (freshRef[0] != null) {
                    mergeWithFreshData(freshRef[0]);
                }
                if (histRef[0] != null && !session.isSeller()) {
                    mergeWithHistory(histRef[0]);
                }
                startCountdown(); // bắt đầu đếm ngược sau khi đã có endTime mới nhất
            });
        }, "fetch-merger-" + auctionId);
        merger.setDaemon(true);
        merger.start();
    }

    /**
     * Merge dữ liệu fresh từ server vào UI.
     *
     * Quy tắc merge — ưu tiên giá trị MỚI HƠN / CAO HƠN:
     *   currentPrice: lấy max(freshData, currentAuction) vì realtime event
     *                 có thể đã cập nhật currentAuction lên cao hơn freshData
     *   totalBids:    lấy max(freshData, currentAuction) cùng lý do
     *   endTime:      lấy giá trị sau hơn (anti-sniping có thể đã gia hạn)
     *   status:       freshData luôn đúng hơn (server là source of truth)
     *   các field tĩnh (name, description, seller...): luôn lấy từ freshData
     *
     * Phải chạy trên FX thread.
     */
    private void mergeWithFreshData(AuctionDto fresh) {
        // Giữ giá cao hơn giữa fresh và những gì realtime đã cập nhật
        BigDecimal mergedPrice = currentAuction.getCurrentPrice() != null
                && currentAuction.getCurrentPrice().compareTo(fresh.getCurrentPrice()) > 0
                ? currentAuction.getCurrentPrice()
                : fresh.getCurrentPrice();

        // totalBids là primitive long — không cần kiểm tra null
        long mergedBids = Math.max(currentAuction.getTotalBids(), fresh.getTotalBids());

        // endTime: lấy giá trị sau hơn (anti-sniping)
        LocalDateTime mergedEnd = (currentAuction.getEndTime() != null
                && fresh.getEndTime() != null
                && currentAuction.getEndTime().isAfter(fresh.getEndTime()))
                ? currentAuction.getEndTime()
                : fresh.getEndTime();

        // Áp các field tĩnh từ fresh
        currentAuction.setName(fresh.getName());
        currentAuction.setDescription(fresh.getDescription());
        currentAuction.setSeller(fresh.getSeller());
        currentAuction.setStartPrice(fresh.getStartPrice());
        currentAuction.setMinIncrement(fresh.getMinIncrement());
        currentAuction.setStartTime(fresh.getStartTime());
        currentAuction.setWinner(fresh.getWinner());

        // Áp các field đã merge
        currentAuction.setCurrentPrice(mergedPrice);
        currentAuction.setTotalBids(mergedBids);
        currentAuction.setEndTime(mergedEnd);

        // Status: fresh luôn là source of truth trừ khi realtime đã CLOSED/CANCELED
        String currentStatus = currentAuction.getStatus();
        boolean alreadyTerminated = "FINISHED".equals(currentStatus)
                || "PAID".equals(currentStatus) || "CANCELED".equals(currentStatus);
        if (!alreadyTerminated) currentAuction.setStatus(fresh.getStatus());

        // Re-render toàn bộ UI với dữ liệu đã merge
        populateInfo();
    }

    /**
     * Merge lịch sử bid: thêm các bid chưa có trong bidData (từ realtime).
     * Tránh duplicate bằng cách so sánh bid id.
     */
    private void mergeWithHistory(List<BidDto> history) {
        if (history == null || history.isEmpty()) return;

        java.util.Set<Long> existingIds = new java.util.HashSet<>();
        for (BidDto b : bidData) {
            if (b.getId() != null) existingIds.add(b.getId());
        }

        // Thêm các bid từ history mà chưa có trong bidData
        // (bidData hiện tại chỉ chứa bid realtime đến sau khi subscribe)
        List<BidDto> toAdd = new java.util.ArrayList<>();
        for (BidDto b : history) {
            if (b.getId() == null || !existingIds.contains(b.getId())) {
                toAdd.add(b);
            }
        }

        if (!toAdd.isEmpty()) {
            bidData.addAll(toAdd);
            // Sort: mới nhất lên đầu
            bidData.sort((a, b) -> {
                if (a.getBidTime() == null) return 1;
                if (b.getBidTime() == null) return -1;
                return b.getBidTime().compareTo(a.getBidTime());
            });
        }

        updateChartFromHistory(new java.util.ArrayList<>(bidData));

        if (!bidData.isEmpty() && bidData.get(0).getBidder() != null)
            lblLeader.setText("Đang dẫn: " + bidData.get(0).getBidder().getUsername());
    }

    // ── Populate UI ───────────────────────────────────────────────────────────

    private void populateInfo() {
        lblAuctionName.setText(currentAuction.getName());
        lblProductName.setText(currentAuction.getName());
        lblDescription.setText(currentAuction.getDescription() != null
                ? currentAuction.getDescription() : "Không có mô tả.");
        lblStartPrice.setText(CURRENCY.format(currentAuction.getStartPrice().longValue()) + "₫");
        lblMinIncrement.setText("+" + CURRENCY.format(currentAuction.getMinIncrement().longValue()) + "₫");
        lblSeller.setText(currentAuction.getSeller() != null
                ? currentAuction.getSeller().getUsername() : "—");
        lblEndTime.setText(currentAuction.getEndTime() != null
                ? currentAuction.getEndTime().format(DTF) : "—");

        refreshLiveStats();
        updateStatusBadge();
        refreshBidControls();
    }

    /**
     * Cập nhật giá, số lượt bid — gọi mỗi khi nhận NEW_BID event.
     * Phải chạy trên FX thread.
     */
    private void refreshLiveStats() {
        lblCurrentPrice.setText(
                CURRENCY.format(currentAuction.getCurrentPrice().longValue()) + "₫");
        lblTotalBids.setText(currentAuction.getTotalBids() + " lượt đặt");
    }

    /**
     * Cập nhật trạng thái hiển thị bidPanel / winnerPanel / txtBidAmount.
     * Gọi khi mở màn hình và khi nhận event làm thay đổi trạng thái phiên.
     */
    private void refreshBidControls() {
        String status = resolveStatus(currentAuction);
        boolean canBid = session.isBidder() && "RUNNING".equals(status);

        bidPanel.setVisible(canBid);
        bidPanel.setManaged(canBid);

        if (canBid) refreshMinBidHint();

        boolean finished = "FINISHED".equals(status) || "PAID".equals(status)
                || "CANCELED".equals(status);
        winnerPanel.setVisible(finished);
        winnerPanel.setManaged(finished);
        if (finished) {
            lblWinner.setText(currentAuction.getWinner() != null
                    ? currentAuction.getWinner().getUsername() : "Không có người thắng");
        }
    }

    /**
     * Cập nhật gợi ý giá tối thiểu trong ô nhập — gọi sau mỗi NEW_BID.
     * Chỉ cập nhật nếu người dùng chưa tự tay nhập giá cao hơn minimum,
     * tránh xóa số họ đang gõ dở.
     */
    private void refreshMinBidHint() {
        BigDecimal minNext = currentAuction.getCurrentPrice()
                .add(currentAuction.getMinIncrement());
        String current = txtBidAmount.getText().trim().replace(",", "");
        boolean userInputHigher = false;
        try {
            userInputHigher = !current.isEmpty()
                    && new BigDecimal(current).compareTo(minNext) >= 0;
        } catch (NumberFormatException ignored) {}

        if (!userInputHigher) {
            txtBidAmount.setText(minNext.toPlainString());
        }
    }

    private void updateStatusBadge() {
        String status = resolveStatus(currentAuction);
        lblStatusBadge.getStyleClass().removeAll(
                "badge-running", "badge-open", "badge-finished", "badge-canceled", "badge");
        lblStatusBadge.getStyleClass().add("badge");
        switch (status == null ? "" : status) {
            case "RUNNING"  -> { lblStatusBadge.setText("● Đang diễn ra");   lblStatusBadge.getStyleClass().add("badge-running");  }
            case "OPEN"     -> { lblStatusBadge.setText("○ Sắp bắt đầu");    lblStatusBadge.getStyleClass().add("badge-open");     }
            case "FINISHED" -> { lblStatusBadge.setText("✓ Đã kết thúc");    lblStatusBadge.getStyleClass().add("badge-finished"); }
            case "PAID"     -> { lblStatusBadge.setText("✓ Đã thanh toán");  lblStatusBadge.getStyleClass().add("badge-finished"); }
            case "CANCELED" -> { lblStatusBadge.setText("✕ Đã hủy");         lblStatusBadge.getStyleClass().add("badge-canceled"); }
            default         -> { lblStatusBadge.setText(status != null ? status : "—"); lblStatusBadge.getStyleClass().add("badge-finished"); }
        }
    }

    // ── Bid table & chart ─────────────────────────────────────────────────────

    private void setupBidTable() {
        colBidTime.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getBidTime() != null
                        ? c.getValue().getBidTime().format(DTF) : ""));
        colBidder.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getBidder() != null
                        ? c.getValue().getBidder().getUsername() : ""));
        colBidAmount.setCellValueFactory(c ->
                new SimpleStringProperty(CURRENCY.format(c.getValue().getAmount().longValue()) + "₫"));
        colBidAuto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isAutoBid() ? "Auto" : ""));
        tblBids.setItems(bidData);
    }

    @FXML public void refreshBidHistory() { fetchFreshDataAndHistory(); }

    private void hideBidHistorySection() {
        tblBids.setVisible(false);
        tblBids.setManaged(false);
        priceChart.setVisible(false);
        priceChart.setManaged(false);
    }

    private void setupChart() {
        xAxis.setLabel("Lượt bid");
        yAxis.setLabel("Giá (₫)");
        priceChart.setLegendVisible(false);
    }

    private void updateChartFromHistory(List<BidDto> bids) {
        priceSeries.getData().clear();
        List<BidDto> ordered = new java.util.ArrayList<>(bids);
        java.util.Collections.reverse(ordered);
        for (int i = 0; i < ordered.size(); i++) {
            priceSeries.getData().add(
                    new XYChart.Data<>(i + 1, ordered.get(i).getAmount().doubleValue()));
        }
    }

    // ── Realtime subscription ─────────────────────────────────────────────────

    private void subscribeRealtime() {
        eventBus.subscribe(currentAuction.getId(), this);
        ws.subscribeToAuction(currentAuction.getId());
    }

    private void unsubscribeRealtime() {
        // FIX: gọi ws.unsubscribeFromAuction một lần duy nhất ở đây,
        // handleBack() cũ gọi hai lần (một lần trong unsubscribeRealtime,
        // một lần nữa bên ngoài) → STOMP gửi UNSUBSCRIBE thừa.
        eventBus.unsubscribe(currentAuction.getId(), this);
        ws.unsubscribeFromAuction(currentAuction.getId());
    }

    /**
     * Entry point của realtime update — được AuctionEventBus gọi trên FX thread.
     *
     * FIX: so sánh Long bằng equals(), không dùng != (so sánh object reference).
     * Long.valueOf(x) != Long.valueOf(y) có thể trả true dù x == y nếu nằm
     * ngoài cache range [-128, 127] → event của phiên hiện tại bị bỏ qua im lặng.
     */
    @Override
    public void onEvent(AuctionEvent event) {
        if (!Long.valueOf(event.getAuctionId()).equals(currentAuction.getId())) return;

        // AuctionEventBus đảm bảo dispatch trên FX thread — có thể thao tác UI trực tiếp
        switch (event.getType()) {
            case NEW_BID         -> handleNewBidEvent(event);
            case AUCTION_CLOSED  -> handleClosedEvent(event);
            case AUCTION_CANCELED -> handleCanceledEvent(event);
            case AUCTION_STARTED -> { /* detail view không cần xử lý */ }
            case AUCTION_CREATED -> { /* detail view không cần xử lý */ }
        }
    }

    /**
     * Xử lý NEW_BID: cập nhật giá, bảng bid, biểu đồ, gợi ý giá tối thiểu.
     *
     * FIX các lỗi so với code cũ:
     *   1. refreshMinBidHint() — cập nhật txtBidAmount nếu user chưa tự nhập giá cao hơn
     *   2. liveRow.setAutoBid() — gán đúng, không để mặc định false
     *   3. Không duplicate unsubscribe trong handleBack()
     */
    private void handleNewBidEvent(AuctionEvent event) {
        // Cập nhật model
        currentAuction.setCurrentPrice(event.getCurrentPrice());
        currentAuction.setTotalBids(event.getTotalBids());
        if (event.getEndTime() != null) currentAuction.setEndTime(event.getEndTime());

        // Cập nhật UI giá và số bid
        refreshLiveStats();

        // Cập nhật leader label
        if (event.getLeaderUsername() != null)
            lblLeader.setText("Đang dẫn: " + event.getLeaderUsername());

        // Anti-sniping: endTime gia hạn → cập nhật label với highlight
        if (event.getEndTime() != null
                && !event.getEndTime().equals(currentAuction.getEndTime())) {
            lblEndTime.setText(event.getEndTime().format(DTF));
            lblEndTime.getStyleClass().removeAll("label", "label-danger");
            lblEndTime.getStyleClass().add("label-danger");
        }

        // Thêm điểm mới lên biểu đồ realtime
        int nextX = priceSeries.getData().size() + 1;
        priceSeries.getData().add(
                new XYChart.Data<>(nextX, event.getCurrentPrice().doubleValue()));

        // Thêm row mới vào bảng bid — đầy đủ field bao gồm isAutoBid
        // (AuctionEvent hiện không mang isAutoBid nên để false;
        //  nếu server sau này thêm field này vào BidUpdateMessage thì update ở đây)
        BidDto liveRow = new BidDto();
        liveRow.setAuctionId(event.getAuctionId());
        liveRow.setAmount(event.getCurrentPrice());
        liveRow.setBidTime(LocalDateTime.now());
        liveRow.setAutoBid(false); // AuctionEvent chưa mang field này
        UserDto bidderDto = new UserDto();
        bidderDto.setUsername(event.getLeaderUsername() != null
                ? event.getLeaderUsername() : "—");
        liveRow.setBidder(bidderDto);
        bidData.add(0, liveRow);

        // Gợi ý giá tối thiểu cho lần bid tiếp theo
        if (bidPanel.isVisible()) refreshMinBidHint();
    }

    private void handleClosedEvent(AuctionEvent event) {
        currentAuction.setCurrentPrice(event.getCurrentPrice());
        currentAuction.setStatus("FINISHED");
        if (event.getEndTime() != null) currentAuction.setEndTime(event.getEndTime());

        refreshLiveStats();
        updateStatusBadge();

        bidPanel.setVisible(false);
        bidPanel.setManaged(false);
        winnerPanel.setVisible(true);
        winnerPanel.setManaged(true);
        lblWinner.setText(event.getLeaderUsername() != null
                ? event.getLeaderUsername() : "Không có người thắng");

        stopCountdown();
        lblCountdown.setText("Đã kết thúc");
        lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
        lblCountdown.getStyleClass().add("label-muted");
    }

    private void handleCanceledEvent(AuctionEvent event) {
        currentAuction.setStatus("CANCELED");
        updateStatusBadge();
        bidPanel.setVisible(false);
        bidPanel.setManaged(false);
        winnerPanel.setVisible(true);
        winnerPanel.setManaged(true);
        lblWinner.setText("Phiên đã bị hủy");
        stopCountdown();
        lblCountdown.setText("Đã hủy");
        lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
        lblCountdown.getStyleClass().add("label-muted");
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startCountdown() {
        stopCountdown();
        if (!"RUNNING".equals(resolveStatus(currentAuction))) return;

        countdownTask = scheduler.scheduleAtFixedRate(() ->
                Platform.runLater(this::tickCountdown),
                0, 1, TimeUnit.SECONDS);
    }

    private void tickCountdown() {
        if (currentAuction.getEndTime() == null) return;
        Duration remaining = Duration.between(LocalDateTime.now(), currentAuction.getEndTime());

        if (remaining.isNegative()) {
            lblCountdown.setText("Đã kết thúc");
            lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
            lblCountdown.getStyleClass().add("label-muted");
            stopCountdown();
            return;
        }

        long h = remaining.toHours();
        long m = remaining.toMinutesPart();
        long s = remaining.toSecondsPart();
        String text = h > 0
                ? String.format("Còn: %02d:%02d:%02d", h, m, s)
                : String.format("Còn: %02d:%02d", m, s);
        lblCountdown.setText(text);

        boolean urgent = remaining.toMinutes() < 2;
        lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
        lblCountdown.getStyleClass().add(urgent ? "countdown-urgent" : "countdown-label");
    }

    /**
     * Dừng countdown task nhưng KHÔNG shutdown scheduler.
     * Scheduler được tái sử dụng nếu người dùng navigate đi rồi quay lại.
     */
    private void stopCountdown() {
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(false); // false: không interrupt nếu đang chạy
            countdownTask = null;
        }
    }

    // ── Bid actions ───────────────────────────────────────────────────────────

    @FXML
    public void handlePlaceBid() {
        String raw = txtBidAmount.getText().trim().replace(",", "");
        if (raw.isEmpty()) { showBidMsg("Vui lòng nhập số tiền.", false); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            showBidMsg("Số tiền không hợp lệ.", false);
            return;
        }

        btnBid.setDisable(true);
        Thread t = new Thread(() -> {
            try {
                BidDto bid = api.placeBid(currentAuction.getId(), amount);
                Platform.runLater(() ->
                        showBidMsg("Đặt giá thành công: "
                                + CURRENCY.format(bid.getAmount().longValue()) + "₫", true));
                // txtBidAmount sẽ được cập nhật bởi handleNewBidEvent khi server broadcast về
            } catch (Exception e) {
                Platform.runLater(() -> showBidMsg(e.getMessage(), false));
            } finally {
                Platform.runLater(() -> btnBid.setDisable(false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleAutoBidOn() {
        String raw = txtAutoMax.getText().trim().replace(",", "");
        if (raw.isEmpty()) { showBidMsg("Nhập giá tối đa cho auto-bid.", false); return; }
        try {
            BigDecimal max = new BigDecimal(raw);
            BigDecimal inc = currentAuction.getMinIncrement();
            Thread t = new Thread(() -> {
                try {
                    api.setupAutoBid(currentAuction.getId(), max, inc);
                    Platform.runLater(() ->
                            showBidMsg("Auto-bid đã bật (tối đa: "
                                    + CURRENCY.format(max.longValue()) + "₫)", true));
                } catch (Exception e) {
                    Platform.runLater(() -> showBidMsg(e.getMessage(), false));
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (NumberFormatException e) {
            showBidMsg("Số tiền tối đa không hợp lệ.", false);
        }
    }

    @FXML
    public void handleAutoBidOff() {
        Thread t = new Thread(() -> {
            try {
                api.cancelAutoBid(currentAuction.getId());
                Platform.runLater(() -> showBidMsg("Auto-bid đã tắt.", true));
            } catch (Exception e) {
                Platform.runLater(() -> showBidMsg(e.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void handleBack() {
        stopCountdown();
        scheduler.shutdownNow(); // shutdown hẳn khi rời màn hình
        unsubscribeRealtime();   // FIX: không gọi ws.unsubscribeFromAuction lần thứ hai
        FxUtil.switchScene(lblAuctionName, "/fxml/auction-list.fxml", "Danh sách đấu giá");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveStatus(AuctionDto a) {
        if (a == null) return "";
        String s = a.getStatus() == null ? "" : a.getStatus();
        if ("FINISHED".equals(s) || "PAID".equals(s) || "CANCELED".equals(s)) return s;
        LocalDateTime now = LocalDateTime.now();
        if ("OPEN".equals(s) || "RUNNING".equals(s)) {
            if (a.getEndTime() != null && now.isAfter(a.getEndTime())) return "FINISHED";
            if ("OPEN".equals(s) && a.getStartTime() != null && now.isAfter(a.getStartTime()))
                return "RUNNING";
        }
        return s;
    }

    private void showBidMsg(String msg, boolean success) {
        lblBidMsg.setText(msg);
        lblBidMsg.getStyleClass().removeAll("label-accent", "label-danger", "label");
        lblBidMsg.getStyleClass().add(success ? "label-accent" : "label-danger");
    }
}
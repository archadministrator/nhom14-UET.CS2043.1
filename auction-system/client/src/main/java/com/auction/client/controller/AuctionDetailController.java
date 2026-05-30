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

    public void setAuction(AuctionDto auction) {
        this.currentAuction = auction;
        Platform.runLater(() -> {
            populateInfo();
            setupBidTable();
            setupChart();
            if (!session.isSeller()) {
                loadBidHistory();
            } else {
                hideBidHistorySection();
            }
            subscribeRealtime();
            startCountdown();
        });
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

    private void loadBidHistory() {
        Thread t = new Thread(() -> {
            try {
                List<BidDto> bids = api.getBidHistory(currentAuction.getId());
                Platform.runLater(() -> {
                    bidData.setAll(bids);
                    updateChartFromHistory(bids);
                    if (!bids.isEmpty() && bids.get(0).getBidder() != null)
                        lblLeader.setText("Đang dẫn: " + bids.get(0).getBidder().getUsername());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showBidMsg("Không tải được lịch sử: " + e.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML public void refreshBidHistory() { loadBidHistory(); }

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
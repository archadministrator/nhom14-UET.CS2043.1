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
import java.util.Timer;
import java.util.TimerTask;

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
    private Timer      countdownTimer;

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

    @FXML
    public void initialize() {
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
    }

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

        updateLivePrice();
        updateStatusBadge();

        String resolvedStatus = resolveStatus(currentAuction);
        boolean canBid = session.isBidder() && "RUNNING".equals(resolvedStatus);
        bidPanel.setVisible(canBid);
        bidPanel.setManaged(canBid);

        if (canBid) {
            BigDecimal minNext = currentAuction.getCurrentPrice()
                    .add(currentAuction.getMinIncrement());
            txtBidAmount.setText(minNext.toPlainString());
        }

        boolean finished = "FINISHED".equals(resolvedStatus)
                || "PAID".equals(resolvedStatus)
                || "CANCELED".equals(resolvedStatus);
        winnerPanel.setVisible(finished);
        winnerPanel.setManaged(finished);
        if (finished && currentAuction.getWinner() != null)
            lblWinner.setText(currentAuction.getWinner().getUsername());
        else if (finished)
            lblWinner.setText("Không có người thắng");
    }

    private void updateLivePrice() {
        lblCurrentPrice.setText(CURRENCY.format(currentAuction.getCurrentPrice().longValue()) + "₫");
        lblTotalBids.setText(currentAuction.getTotalBids() + " lượt đặt");
    }

    /**
     * Update the status badge using resolveStatus (time-aware) — no inline styles.
     */
    private void updateStatusBadge() {
        String status = resolveStatus(currentAuction);
        lblStatusBadge.getStyleClass().removeAll(
                "badge-running", "badge-open", "badge-finished", "badge-canceled", "badge");
        lblStatusBadge.getStyleClass().add("badge");

        String text;
        String cssClass;
        switch (status == null ? "" : status) {
            case "RUNNING"  -> { text = "● Đang diễn ra"; cssClass = "badge-running"; }
            case "OPEN"     -> { text = "○ Sắp bắt đầu";  cssClass = "badge-open";    }
            case "FINISHED" -> { text = "✓ Đã kết thúc";  cssClass = "badge-finished"; }
            case "PAID"     -> { text = "✓ Đã thanh toán"; cssClass = "badge-finished"; }
            case "CANCELED" -> { text = "✕ Đã hủy";       cssClass = "badge-canceled"; }
            default         -> { text = status != null ? status : "—"; cssClass = "badge-finished"; }
        }
        lblStatusBadge.setText(text);
        lblStatusBadge.getStyleClass().add(cssClass);
    }

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
                    updateChart(bids);
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

    private void updateChart(List<BidDto> bids) {
        priceSeries.getData().clear();
        List<BidDto> ordered = new java.util.ArrayList<>(bids);
        java.util.Collections.reverse(ordered);
        for (int i = 0; i < ordered.size(); i++) {
            priceSeries.getData().add(new XYChart.Data<>(
                    i + 1, ordered.get(i).getAmount().doubleValue()));
        }
    }

    private void subscribeRealtime() {
        eventBus.subscribe(currentAuction.getId(), this);
        ws.subscribeToAuction(currentAuction.getId());
    }

    private void unsubscribeRealtime() {
        eventBus.unsubscribe(currentAuction.getId(), this);
        ws.unsubscribeFromAuction(currentAuction.getId());
    }

    @Override
    public void onEvent(AuctionEvent event) {
        if (event.getAuctionId() != currentAuction.getId()) return;

        switch (event.getType()) {
            case NEW_BID        -> handleNewBidEvent(event);
            case AUCTION_CLOSED -> handleClosedEvent(event);
            case AUCTION_STARTED -> { /* no-op in detail view */ }
        }
    }

    private void handleNewBidEvent(AuctionEvent event) {
        currentAuction.setCurrentPrice(event.getCurrentPrice());
        currentAuction.setTotalBids(event.getTotalBids());

        // Anti-sniping: end time extended
        if (event.getEndTime() != null
                && !event.getEndTime().equals(currentAuction.getEndTime())) {
            currentAuction.setEndTime(event.getEndTime());
            // Mark end time label red/urgent via style class swap
            lblEndTime.setText(event.getEndTime().format(DTF));
            lblEndTime.getStyleClass().removeAll("label", "label-danger");
            lblEndTime.getStyleClass().add("label-danger");
        }

        updateLivePrice();
        if (event.getLeaderUsername() != null)
            lblLeader.setText("Đang dẫn: " + event.getLeaderUsername());

        int nextX = priceSeries.getData().size() + 1;
        priceSeries.getData().add(
                new XYChart.Data<>(nextX, event.getCurrentPrice().doubleValue()));

        BidDto liveRow = new BidDto();
        liveRow.setAuctionId(event.getAuctionId());
        liveRow.setAmount(event.getCurrentPrice());
        liveRow.setBidTime(LocalDateTime.now());
        UserDto bidderDto = new UserDto();
        bidderDto.setUsername(event.getLeaderUsername() != null ? event.getLeaderUsername() : "—");
        liveRow.setBidder(bidderDto);
        bidData.add(0, liveRow);
    }

    private void handleClosedEvent(AuctionEvent event) {
        currentAuction.setCurrentPrice(event.getCurrentPrice());
        currentAuction.setStatus("FINISHED");
        updateStatusBadge();
        updateLivePrice();

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

    private void startCountdown() {
        stopCountdown();
        if (!"RUNNING".equals(resolveStatus(currentAuction))) return;

        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentAuction.getEndTime() == null) return;
                    Duration remaining = Duration.between(
                            LocalDateTime.now(), currentAuction.getEndTime());
                    if (remaining.isNegative()) {
                        lblCountdown.setText("Đã kết thúc");
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

                    // Swap CSS class when urgent (<2 min)
                    boolean urgent = remaining.toMinutes() < 2;
                    lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
                    lblCountdown.getStyleClass().add(urgent ? "countdown-urgent" : "countdown-label");
                });
            }
        }, 0, 1000);
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

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
                Platform.runLater(() -> {
                    showBidMsg("Đặt giá thành công: " + CURRENCY.format(bid.getAmount().longValue()) + "₫", true);
                    BigDecimal next = amount.add(currentAuction.getMinIncrement());
                    txtBidAmount.setText(next.toPlainString());
                });
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
                            showBidMsg("Auto-bid đã bật (tối đa: " + CURRENCY.format(max.longValue()) + "₫)", true));
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

    @FXML
    public void handleBack() {
        stopCountdown();
        unsubscribeRealtime();
        ws.unsubscribeFromAuction(currentAuction.getId());
        FxUtil.switchScene(lblAuctionName, "/fxml/auction-list.fxml", "Danh sách đấu giá");
    }

    /**
     * Tính lại status thực tế dựa trên thời gian hiện tại — giống AuctionListController.
     */
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

    /**
     * Show bid message using CSS classes — avoids inline style strings.
     * success → label-accent (green), failure → label-danger (red)
     */
    private void showBidMsg(String msg, boolean success) {
        lblBidMsg.setText(msg);
        lblBidMsg.getStyleClass().removeAll("label-accent", "label-danger", "label");
        lblBidMsg.getStyleClass().add(success ? "label-accent" : "label-danger");
    }
}

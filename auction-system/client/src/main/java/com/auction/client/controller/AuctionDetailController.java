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

        boolean canBid = session.isBidder() && currentAuction.isRunning();
        bidPanel.setVisible(canBid);
        bidPanel.setManaged(canBid);

        if (canBid) {
            BigDecimal minNext = currentAuction.getCurrentPrice()
                    .add(currentAuction.getMinIncrement());
            txtBidAmount.setText(minNext.toPlainString());
        }

        boolean finished = currentAuction.isFinished();
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

    private void updateStatusBadge() {
        String status = currentAuction.getStatus();
        String text = translateStatus(status);
        String bg = switch (status == null ? "" : status) {
            case "RUNNING"  -> "#43A047";
            case "OPEN"     -> "#FB8C00";
            case "FINISHED", "PAID" -> "#757575";
            case "CANCELED" -> "#E53935";
            default         -> "#9E9E9E";
        };
        lblStatusBadge.setText(text);
        lblStatusBadge.setStyle("-fx-font-size:12px;-fx-text-fill:white;-fx-padding:4 12;"
                + "-fx-background-radius:12;-fx-background-color:" + bg + ";");
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
                Platform.runLater(() -> lblBidMsg.setText("Không tải được lịch sử: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML public void refreshBidHistory() { loadBidHistory(); }

    /** Ẩn hoàn toàn section lịch sử bid cho SELLER (không có quyền xem) */
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
        // Đăng ký Observer vào EventBus — không dùng callback trực tiếp
        eventBus.subscribe(currentAuction.getId(), this);
        // Yêu cầu WebSocketService gửi STOMP SUBSCRIBE frame
        ws.subscribeToAuction(currentAuction.getId());
    }

    private void unsubscribeRealtime() {
        eventBus.unsubscribe(currentAuction.getId(), this);
        ws.unsubscribeFromAuction(currentAuction.getId());
    }

    /**
     * Observer callback — đảm bảo chạy trên FX Thread bởi AuctionEventBus.
     * Không gọi HTTP, không polling — chỉ cập nhật UI từ dữ liệu trong event.
     */
    @Override
    public void onEvent(AuctionEvent event) {
        // Bỏ qua event của phiên khác (phòng trường hợp global bus)
        if (event.getAuctionId() != currentAuction.getId()) return;

        switch (event.getType()) {
            case NEW_BID -> handleNewBidEvent(event);
            case AUCTION_CLOSED -> handleClosedEvent(event);
            case AUCTION_STARTED -> { /* không cần xử lý trong detail view */ }
        }
    }

    private void handleNewBidEvent(AuctionEvent event) {
        // Cập nhật model
        currentAuction.setCurrentPrice(event.getCurrentPrice());
        currentAuction.setTotalBids(event.getTotalBids());

        // Anti-sniping: endTime bị gia hạn
        if (event.getEndTime() != null
                && !event.getEndTime().equals(currentAuction.getEndTime())) {
            currentAuction.setEndTime(event.getEndTime());
            lblEndTime.setText(event.getEndTime().format(DTF));
            lblEndTime.setStyle("-fx-text-fill:#FF5252;-fx-font-weight:bold;");
        }

        // Cập nhật labels
        updateLivePrice();
        if (event.getLeaderUsername() != null)
            lblLeader.setText("Đang dẫn: " + event.getLeaderUsername());

        // Thêm điểm mới vào chart (không reload toàn bộ)
        int nextX = priceSeries.getData().size() + 1;
        priceSeries.getData().add(
                new XYChart.Data<>(nextX, event.getCurrentPrice().doubleValue()));

        // Thêm hàng mới vào bảng — tạo BidDto tạm từ event data, KHÔNG gọi HTTP
        BidDto liveRow = new BidDto();
        liveRow.setAuctionId(event.getAuctionId());
        liveRow.setAmount(event.getCurrentPrice());
        liveRow.setBidTime(java.time.LocalDateTime.now());
        UserDto bidderDto = new UserDto();
        bidderDto.setUsername(event.getLeaderUsername() != null ? event.getLeaderUsername() : "—");
        liveRow.setBidder(bidderDto);
        // Chèn đầu danh sách (bid mới nhất lên trên)
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
    }

    private void startCountdown() {
        stopCountdown();
        if (!currentAuction.isRunning()) return;

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
                    String color = remaining.toMinutes() < 2 ? "#FF5252" : "#FFD54F";
                    lblCountdown.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
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

    private void showBidMsg(String msg, boolean success) {
        lblBidMsg.setText(msg);
        lblBidMsg.setStyle("-fx-font-size:12px;-fx-text-fill:" + (success ? "#43A047" : "#E53935") + ";");
    }

    private String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "OPEN"     -> "Sắp bắt đầu";
            case "RUNNING"  -> "Đang diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "PAID"     -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            default         -> s;
        };
    }
}
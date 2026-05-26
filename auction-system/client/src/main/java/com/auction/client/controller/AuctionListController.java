package com.auction.client.controller;

import com.auction.client.model.ClientDto.*;
import com.auction.client.realtime.AuctionEvent;
import com.auction.client.realtime.AuctionEventBus;
import com.auction.client.service.ApiService;
import com.auction.client.service.SessionManager;
import com.auction.client.service.WebSocketService;
import com.auction.client.util.FxUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AuctionListController {

    @FXML private Label  lblUsername, lblRole, lblBalance, lblWsStatus;
    @FXML private Label  lblPageTitle, lblCount, lblStatus;
    @FXML private Button btnTopUp, btnMySales, btnNewAuction;
    @FXML private TextField txtSearch;

    @FXML private TableView<AuctionDto>          tblAuctions;
    @FXML private TableColumn<AuctionDto,String> colId, colName, colPrice;
    @FXML private TableColumn<AuctionDto,String> colBids, colStatus, colEndTime, colSeller;

    private final ObservableList<AuctionDto> auctionData = FXCollections.observableArrayList();
    private final ApiService      api      = ApiService.getInstance();
    private final SessionManager  session  = SessionManager.getInstance();
    private final WebSocketService ws      = WebSocketService.getInstance();
    private final AuctionEventBus  eventBus = AuctionEventBus.getInstance();

    private static final NumberFormat CURRENCY =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        setupNavBar();
        setupTable();
        loadAuctions();
        connectWebSocket();
    }

    private void setupNavBar() {
        lblUsername.setText(session.getUsername());
        lblRole.setText(session.getRole());
        lblBalance.setText("Số dư: " + CURRENCY.format((long) session.getBalance()) + "₫");

        boolean isSeller = session.isSeller() || session.isAdmin();
        btnMySales.setVisible(isSeller);
        btnMySales.setManaged(isSeller);
        btnNewAuction.setVisible(isSeller);
        btnNewAuction.setManaged(isSeller);
        btnTopUp.setVisible(session.isBidder());
        btnTopUp.setManaged(session.isBidder());
    }

    private void setupTable() {
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));
        colPrice.setCellValueFactory(c ->
                new SimpleStringProperty(CURRENCY.format(c.getValue().getCurrentPrice().longValue()) + "₫"));
        colBids.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getTotalBids())));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(translateStatus(c.getValue().getStatus())));
        colEndTime.setCellValueFactory(c -> {
            if (c.getValue().getEndTime() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(c.getValue().getEndTime().format(DTF));
        });
        colSeller.setCellValueFactory(c -> {
            UserDto seller = c.getValue().getSeller();
            return new SimpleStringProperty(seller != null ? seller.getUsername() : "—");
        });

        // Dark-theme row highlighting
        tblAuctions.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AuctionDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-running", "row-finished");
                if (!empty && item != null) {
                    if ("RUNNING".equals(item.getStatus()))
                        getStyleClass().add("row-running");
                    else if (item.isFinished())
                        getStyleClass().add("row-finished");
                }
            }
        });

        tblAuctions.setItems(auctionData);
    }

    private void connectWebSocket() {
        setWsStatus("○ Đang kết nối...", "ws-dot-pending");

        eventBus.subscribeGlobal(event -> {
            updateAuctionRow(event);
            setWsStatus("● Trực tiếp", "ws-dot-live");
        });

        ws.subscribeGlobal();
        ws.connect();

        Thread checker = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                if (ws.isConnected())
                    setWsStatus("● Đã kết nối", "ws-dot-live");
                else
                    setWsStatus("✕ Mất kết nối", "ws-dot-off");
            });
        });
        checker.setDaemon(true);
        checker.start();
    }

    /** Update WebSocket status label using CSS classes instead of inline styles */
    private void setWsStatus(String text, String cssClass) {
        lblWsStatus.setText(text);
        lblWsStatus.getStyleClass().removeAll("ws-dot-live", "ws-dot-pending", "ws-dot-off");
        lblWsStatus.getStyleClass().add(cssClass);
    }

    private void updateAuctionRow(AuctionEvent event) {
        for (int i = 0; i < auctionData.size(); i++) {
            AuctionDto a = auctionData.get(i);
            if (a.getId().equals(event.getAuctionId())) {
                a.setCurrentPrice(event.getCurrentPrice());
                a.setTotalBids(event.getTotalBids());
                if (event.getEndTime() != null) a.setEndTime(event.getEndTime());
                if (event.getType() == AuctionEvent.Type.AUCTION_CLOSED)  a.setStatus("FINISHED");
                if (event.getType() == AuctionEvent.Type.AUCTION_STARTED) a.setStatus("RUNNING");
                break;
            }
        }
        tblAuctions.refresh();
    }

    private void loadAuctions() {
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            Platform.runLater(() -> {
                auctionData.setAll(list);
                lblCount.setText(list.size() + " phiên");
            });
        });
    }

    @FXML public void filterAll() {
        lblPageTitle.setText("Tất cả phiên đấu giá");
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            Platform.runLater(() -> { auctionData.setAll(list); lblCount.setText(list.size() + " phiên"); });
        });
    }

    @FXML public void filterRunning() {
        lblPageTitle.setText("Đang diễn ra");
        runAsync(() -> {
            List<AuctionDto> list = api.getActiveAuctions();
            Platform.runLater(() -> { auctionData.setAll(list); lblCount.setText(list.size() + " phiên"); });
        });
    }

    @FXML public void filterOpen() {
        lblPageTitle.setText("Sắp diễn ra");
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            List<AuctionDto> filtered = list.stream()
                    .filter(a -> "OPEN".equals(a.getStatus())).toList();
            Platform.runLater(() -> { auctionData.setAll(filtered); lblCount.setText(filtered.size() + " phiên"); });
        });
    }

    @FXML public void filterFinished() {
        lblPageTitle.setText("Đã kết thúc");
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            List<AuctionDto> filtered = list.stream()
                    .filter(AuctionDto::isFinished).toList();
            Platform.runLater(() -> { auctionData.setAll(filtered); lblCount.setText(filtered.size() + " phiên"); });
        });
    }

    @FXML public void showMySales() {
        lblPageTitle.setText("Sản phẩm của tôi");
        runAsync(() -> {
            List<AuctionDto> list = api.getMySales();
            Platform.runLater(() -> { auctionData.setAll(list); lblCount.setText(list.size() + " phiên"); });
        });
    }

    @FXML public void showMyBids() {
        lblPageTitle.setText("Lịch sử bid của tôi");
        runAsync(() -> {
            List<BidDto> bids = api.getMyBids();
            List<Long> ids = bids.stream().map(BidDto::getAuctionId).distinct().toList();
            List<AuctionDto> auctions = ids.stream().map(id -> {
                try { return api.getAuction(id); } catch (Exception e) { return null; }
            }).filter(a -> a != null).toList();
            Platform.runLater(() -> { auctionData.setAll(auctions); lblCount.setText(auctions.size() + " phiên"); });
        });
    }

    @FXML public void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) { filterAll(); return; }
        runAsync(() -> {
            List<AuctionDto> list = api.searchAuctions(keyword);
            Platform.runLater(() -> { auctionData.setAll(list); lblCount.setText(list.size() + " kết quả"); });
        });
    }

    @FXML public void handleRefresh() {
        filterAll();
        lblStatus.setText("Đã cập nhật");
    }

    @FXML
    public void handleRowDoubleClick(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            AuctionDto selected = tblAuctions.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            FxUtil.switchSceneWithData(tblAuctions, "/fxml/auction-detail.fxml",
                    selected.getName(),
                    ctrl -> ((AuctionDetailController) ctrl).setAuction(selected));
        }
    }

    @FXML
    public void handleCreateAuction() {
        FxUtil.switchScene(tblAuctions, "/fxml/seller-dashboard.fxml", "Quản lý sản phẩm");
    }

    @FXML
    public void handleTopUp() {
        FxUtil.showInputDialog("Nạp tiền", "Nhập số tiền muốn nạp (₫)", "1000000")
                .ifPresent(input -> {
                    try {
                        BigDecimal amount = new BigDecimal(input.trim().replace(",", ""));
                        runAsync(() -> {
                            UserDto user = api.topUp(amount);
                            Platform.runLater(() -> {
                                session.setBalance(user.getBalance().doubleValue());
                                lblBalance.setText("Số dư: " + CURRENCY.format((long) session.getBalance()) + "₫");
                                lblStatus.setText("Nạp tiền thành công");
                            });
                        });
                    } catch (NumberFormatException ex) {
                        FxUtil.showError("Số tiền không hợp lệ.");
                    }
                });
    }

    @FXML
    public void handleLogout() {
        if (FxUtil.showConfirm("Bạn có chắc muốn đăng xuất?")) {
            ws.disconnect();
            session.logout();
            FxUtil.switchScene(lblUsername, "/fxml/login.fxml", "Đăng nhập");
        }
    }

    private void runAsync(ThrowingRunnable task) {
        Thread t = new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError(e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String translateStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "OPEN"     -> "○ Sắp bắt đầu";
            case "RUNNING"  -> "● Đang diễn ra";
            case "FINISHED" -> "✓ Đã kết thúc";
            case "PAID"     -> "✓ Đã thanh toán";
            case "CANCELED" -> "✕ Đã hủy";
            default         -> status;
        };
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}

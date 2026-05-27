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
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AuctionListController {

    @FXML private Label  lblUsername, lblRole, lblBalance, lblWsStatus;
    @FXML private Label  lblPageTitle, lblCount, lblStatus;
    @FXML private Button btnTopUp, btnNewAuction;
    @FXML private TextField txtSearch;

    // Sidebar filter buttons — cần inject để đổi active style
    @FXML private Button btnMenuAll, btnMenuRunning, btnMenuOpen, btnMenuFinished;
    @FXML private Button btnMenuMyBids, btnMenuMySales;

    @FXML private TableView<AuctionDto>          tblAuctions;
    @FXML private TableColumn<AuctionDto,String> colId, colName, colPrice;
    @FXML private TableColumn<AuctionDto,String> colBids, colStatus, colEndTime, colSeller;

    private final ObservableList<AuctionDto> auctionData = FXCollections.observableArrayList();
    private final ApiService      api      = ApiService.getInstance();
    private final SessionManager  session  = SessionManager.getInstance();
    private final WebSocketService ws      = WebSocketService.getInstance();
    private final AuctionEventBus  eventBus = AuctionEventBus.getInstance();

    // Giữ reference để unsubscribe khi rời màn hình — tránh leak
    private AuctionObserver globalObserver;

    // Track tab hiện tại để handleRefresh biết cần reload gì
    private Runnable activeFilter;

    private static final NumberFormat CURRENCY =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        setupNavBar();
        setupTable();
        connectWebSocket();
        filterAll(); // tab mặc định
    }

    private void setupNavBar() {
        lblUsername.setText(session.getUsername());
        lblRole.setText(session.getRole());
        lblBalance.setText("Số dư: " + CURRENCY.format((long) session.getBalance()) + "₫");

        boolean isSeller = session.isSeller() || session.isAdmin();
        btnMenuMySales.setVisible(isSeller);
        btnMenuMySales.setManaged(isSeller);
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
                new SimpleStringProperty(translateStatus(resolveStatus(c.getValue()))));
        colEndTime.setCellValueFactory(c -> {
            if (c.getValue().getEndTime() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(c.getValue().getEndTime().format(DTF));
        });
        colSeller.setCellValueFactory(c -> {
            UserDto seller = c.getValue().getSeller();
            return new SimpleStringProperty(seller != null ? seller.getUsername() : "—");
        });

        tblAuctions.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AuctionDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-running", "row-finished");
                if (!empty && item != null) {
                    String resolved = resolveStatus(item);
                    if ("RUNNING".equals(resolved))
                        getStyleClass().add("row-running");
                    else if ("FINISHED".equals(resolved) || "PAID".equals(resolved)
                            || "CANCELED".equals(resolved))
                        getStyleClass().add("row-finished");
                }
            }
        });

        tblAuctions.setItems(auctionData);
    }

    // ── Sidebar active state ─────────────────────────────────────────────────

    private Button[] allSidebarBtns() {
        return new Button[]{ btnMenuAll, btnMenuRunning, btnMenuOpen,
                             btnMenuFinished, btnMenuMyBids, btnMenuMySales };
    }

    private void setActiveSidebarBtn(Button active) {
        for (Button b : allSidebarBtns()) {
            if (b == null) continue;
            b.getStyleClass().removeAll("sidebar-btn-active", "sidebar-btn");
            b.getStyleClass().add(b == active ? "sidebar-btn-active" : "sidebar-btn");
        }
    }

    // ── Filter methods ───────────────────────────────────────────────────────

    @FXML public void filterAll() {
        setActiveSidebarBtn(btnMenuAll);
        lblPageTitle.setText("Tất cả phiên đấu giá");
        activeFilter = this::filterAll;
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            Platform.runLater(() -> {
                auctionData.setAll(list);
                lblCount.setText(list.size() + " phiên");
            });
        });
    }

    @FXML public void filterRunning() {
        setActiveSidebarBtn(btnMenuRunning);
        lblPageTitle.setText("Đang diễn ra");
        activeFilter = this::filterRunning;
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            List<AuctionDto> filtered = list.stream()
                    .filter(a -> "RUNNING".equals(resolveStatus(a))).toList();
            Platform.runLater(() -> {
                auctionData.setAll(filtered);
                lblCount.setText(filtered.size() + " phiên");
            });
        });
    }

    @FXML public void filterOpen() {
        setActiveSidebarBtn(btnMenuOpen);
        lblPageTitle.setText("Sắp diễn ra");
        activeFilter = this::filterOpen;
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            List<AuctionDto> filtered = list.stream()
                    .filter(a -> "OPEN".equals(resolveStatus(a))).toList();
            Platform.runLater(() -> {
                auctionData.setAll(filtered);
                lblCount.setText(filtered.size() + " phiên");
            });
        });
    }

    @FXML public void filterFinished() {
        setActiveSidebarBtn(btnMenuFinished);
        lblPageTitle.setText("Đã kết thúc");
        activeFilter = this::filterFinished;
        runAsync(() -> {
            List<AuctionDto> list = api.getAllAuctions();
            List<AuctionDto> filtered = list.stream()
                    .filter(a -> {
                        String s = resolveStatus(a);
                        return "FINISHED".equals(s) || "PAID".equals(s) || "CANCELED".equals(s);
                    }).toList();
            Platform.runLater(() -> {
                auctionData.setAll(filtered);
                lblCount.setText(filtered.size() + " phiên");
            });
        });
    }

    @FXML public void showMyBids() {
        setActiveSidebarBtn(btnMenuMyBids);
        lblPageTitle.setText("Lịch sử bid của tôi");
        activeFilter = this::showMyBids;
        runAsync(() -> {
            List<BidDto> bids = api.getMyBids();
            List<Long> ids = bids.stream().map(BidDto::getAuctionId).distinct().toList();
            List<AuctionDto> auctions = ids.stream()
                    .map(id -> { try { return api.getAuction(id); } catch (Exception e) { return null; } })
                    .filter(a -> a != null).toList();
            Platform.runLater(() -> {
                auctionData.setAll(auctions);
                lblCount.setText(auctions.size() + " phiên");
            });
        });
    }

    @FXML public void showMySales() {
        setActiveSidebarBtn(btnMenuMySales);
        lblPageTitle.setText("Sản phẩm của tôi");
        activeFilter = this::showMySales;
        runAsync(() -> {
            List<AuctionDto> list = api.getMySales();
            Platform.runLater(() -> {
                auctionData.setAll(list);
                lblCount.setText(list.size() + " phiên");
            });
        });
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    @FXML public void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) { filterAll(); return; }
        // Tìm kiếm không thuộc filter nào → bỏ active hết
        setActiveSidebarBtn(null);
        lblPageTitle.setText("Kết quả tìm kiếm: \"" + keyword + "\"");
        activeFilter = this::handleSearch;
        runAsync(() -> {
            List<AuctionDto> list = api.searchAuctions(keyword);
            Platform.runLater(() -> {
                auctionData.setAll(list);
                lblCount.setText(list.size() + " kết quả");
            });
        });
    }

    /** Refresh lại đúng tab đang active, không nhảy về tab khác */
    @FXML public void handleRefresh() {
        if (activeFilter != null) activeFilter.run();
        lblStatus.setText("Đã cập nhật");
    }

    // ── Row double-click ─────────────────────────────────────────────────────

    @FXML
    public void handleRowDoubleClick(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            AuctionDto selected = tblAuctions.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            cleanupObserver();
            FxUtil.switchSceneWithData(tblAuctions, "/fxml/auction-detail.fxml",
                    selected.getName(),
                    ctrl -> ((AuctionDetailController) ctrl).setAuction(selected));
        }
    }

    @FXML public void handleCreateAuction() {
        cleanupObserver();
        FxUtil.switchScene(tblAuctions, "/fxml/seller-dashboard.fxml", "Quản lý sản phẩm");
    }

    // ── Top nav ──────────────────────────────────────────────────────────────

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
            cleanupObserver();
            ws.disconnect();
            session.logout();
            FxUtil.switchScene(lblUsername, "/fxml/login.fxml", "Đăng nhập");
        }
    }

    // ── WebSocket ────────────────────────────────────────────────────────────

    private void connectWebSocket() {
        setWsStatus("○ Đang kết nối...", "ws-dot-pending");

        globalObserver = event -> {
            switch (event.getType()) {
                case NEW_BID, AUCTION_STARTED, AUCTION_CLOSED,
                     AUCTION_CANCELED                         -> updateAuctionRow(event);
                case AUCTION_CREATED                          -> addNewAuctionRow(event);
            }
            setWsStatus("● Trực tiếp", "ws-dot-live");
        };

        eventBus.subscribeGlobal(globalObserver);
        ws.subscribeGlobal();
        ws.connect();

        Thread checker = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                if (ws.isConnected()) setWsStatus("● Đã kết nối", "ws-dot-live");
                else                  setWsStatus("✕ Mất kết nối", "ws-dot-off");
            });
        });
        checker.setDaemon(true);
        checker.start();
    }

    private void cleanupObserver() {
        if (globalObserver != null) {
            eventBus.unsubscribeGlobal(globalObserver);
            globalObserver = null;
        }
    }

    private void setWsStatus(String text, String cssClass) {
        lblWsStatus.setText(text);
        lblWsStatus.getStyleClass().removeAll("ws-dot-live", "ws-dot-pending", "ws-dot-off");
        lblWsStatus.getStyleClass().add(cssClass);
    }

    /**
     * Phiên mới vừa được seller tạo → fetch đầy đủ từ API rồi thêm vào đầu list.
     * Chạy trên FX thread (đảm bảo bởi EventBus.publish).
     */
    private void addNewAuctionRow(AuctionEvent event) {
        boolean exists = auctionData.stream()
                .anyMatch(a -> a.getId() != null && a.getId() == event.getAuctionId());
        if (exists) return;

        Thread t = new Thread(() -> {
            try {
                AuctionDto dto = api.getAuction(event.getAuctionId());
                Platform.runLater(() -> {
                    // Kiểm tra filter hiện tại có cho phép phiên này không
                    if (shouldShowInCurrentFilter(dto)) {
                        auctionData.add(0, dto);
                        lblCount.setText(auctionData.size() + " phiên");
                    }
                });
            } catch (Exception e) {
                System.err.println("[AuctionList] Cannot fetch new auction: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** Trả về true nếu phiên dto phù hợp với filter tab hiện tại */
    private boolean shouldShowInCurrentFilter(AuctionDto dto) {
        if (activeFilter == null) return true;
        String resolved = resolveStatus(dto);
        // So sánh bằng method reference identity không đáng tin cậy —
        // dùng lblPageTitle để nhận biết tab đang active
        String title = lblPageTitle.getText();
        return switch (title) {
            case "Tất cả phiên đấu giá"  -> true;
            case "Đang diễn ra"           -> "RUNNING".equals(resolved);
            case "Sắp diễn ra"            -> "OPEN".equals(resolved);
            case "Đã kết thúc"            -> "FINISHED".equals(resolved)
                    || "PAID".equals(resolved) || "CANCELED".equals(resolved);
            case "Sản phẩm của tôi"       -> dto.getSeller() != null
                    && dto.getSeller().getUsername().equals(session.getUsername());
            default -> true; // tìm kiếm hoặc lịch sử bid → không push realtime
        };
    }

    private void updateAuctionRow(AuctionEvent event) {
        for (AuctionDto a : auctionData) {
            if (a.getId() != null && a.getId() == event.getAuctionId()) {
                // Cập nhật giá và số lượt bid
                a.setCurrentPrice(event.getCurrentPrice());
                a.setTotalBids(event.getTotalBids());
                if (event.getEndTime() != null) a.setEndTime(event.getEndTime());

                // Cập nhật status field để resolveStatus() render đúng ngay lập tức
                switch (event.getType()) {
                    case AUCTION_STARTED  -> a.setStatus("RUNNING");
                    case AUCTION_CLOSED   -> a.setStatus("FINISHED");
                    case AUCTION_CANCELED -> a.setStatus("CANCELED");
                    default -> {} // NEW_BID: giữ nguyên status
                }
                break;
            }
        }
        // refresh() kích hoạt cellValueFactory chạy lại → resolveStatus() được gọi lại
        tblAuctions.refresh();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void runAsync(ThrowingRunnable task) {
        Thread t = new Thread(() -> {
            try { task.run(); }
            catch (Exception e) { Platform.runLater(() -> FxUtil.showError(e.getMessage())); }
        });
        t.setDaemon(true);
        t.start();
    }

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
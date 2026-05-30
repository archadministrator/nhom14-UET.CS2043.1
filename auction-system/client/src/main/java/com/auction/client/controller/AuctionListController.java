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
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionListController {

    @FXML private Label  lblUsername, lblRole, lblBalance, lblWsStatus;
    @FXML private Label  lblPageTitle, lblCount, lblStatus;
    @FXML private Button btnTopUp, btnNewAuction;
    @FXML private TextField txtSearch;

    @FXML private Button btnMenuAll, btnMenuRunning, btnMenuOpen, btnMenuFinished;
    @FXML private Button btnMenuMyBids, btnMenuMySales;

    @FXML private TableView<AuctionDto>          tblAuctions;
    @FXML private TableColumn<AuctionDto,String> colId, colName, colPrice;
    @FXML private TableColumn<AuctionDto,String> colBids, colStatus, colEndTime, colSeller;

    // ── Reactive price/bid cache ─────────────────────────────────────────────
    // Mỗi auctionId → SimpleObjectProperty<BigDecimal> cho currentPrice
    // Mỗi auctionId → SimpleObjectProperty<Long> cho totalBids
    // TableView bind vào các property này → tự refresh khi property thay đổi,
    // không cần gọi tblAuctions.refresh() thủ công.
    private final Map<Long, SimpleObjectProperty<BigDecimal>> priceProps
            = new ConcurrentHashMap<>();
    private final Map<Long, SimpleObjectProperty<Long>> bidCountProps
            = new ConcurrentHashMap<>();
    private final Map<Long, SimpleObjectProperty<String>> statusProps
            = new ConcurrentHashMap<>();

    // ObservableList với extractor: khai báo Observable[] cho mỗi phần tử.
    // Khi field trong extractor thay đổi, TableView nhận invalidation → refresh row.
    // Dùng kết hợp với property map ở trên để đảm bảo cả hai cơ chế hoạt động.
    private final ObservableList<AuctionDto> auctionData =
            FXCollections.observableArrayList(item -> new Observable[]{
                    getPriceProp(item),
                    getBidCountProp(item),
                    getStatusProp(item)
            });

    private final ApiService      api      = ApiService.getInstance();
    private final SessionManager  session  = SessionManager.getInstance();
    private final WebSocketService ws      = WebSocketService.getInstance();
    private final AuctionEventBus  eventBus = AuctionEventBus.getInstance();

    private AuctionObserver globalObserver;
    private Runnable activeFilter;

    private static final NumberFormat CURRENCY =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // ── Property helpers ─────────────────────────────────────────────────────

    private SimpleObjectProperty<BigDecimal> getPriceProp(AuctionDto a) {
        return priceProps.computeIfAbsent(a.getId(),
                id -> new SimpleObjectProperty<>(a.getCurrentPrice()));
    }

    private SimpleObjectProperty<Long> getBidCountProp(AuctionDto a) {
        return bidCountProps.computeIfAbsent(a.getId(),
                id -> new SimpleObjectProperty<>(a.getTotalBids()));
    }

    private SimpleObjectProperty<String> getStatusProp(AuctionDto a) {
        return statusProps.computeIfAbsent(a.getId(),
                id -> new SimpleObjectProperty<>(resolveStatus(a)));
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupNavBar();
        setupTable();
        connectWebSocket();
        filterAll();
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
                getPriceProp(c.getValue()).map(price ->
                        price == null ? "—" : CURRENCY.format(price.longValue()) + "₫"));

        colBids.setCellValueFactory(c ->
                getBidCountProp(c.getValue()).map(String::valueOf));

        colStatus.setCellValueFactory(c ->
                getStatusProp(c.getValue()).map(this::translateStatus));

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
            Platform.runLater(() -> loadData(list, list.size() + " phiên"));
        });
    }

    @FXML public void filterRunning() {
        setActiveSidebarBtn(btnMenuRunning);
        lblPageTitle.setText("Đang diễn ra");
        activeFilter = this::filterRunning;
        runAsync(() -> {
            List<AuctionDto> filtered = api.getAllAuctions().stream()
                    .filter(a -> "RUNNING".equals(resolveStatus(a))).toList();
            Platform.runLater(() -> loadData(filtered, filtered.size() + " phiên"));
        });
    }

    @FXML public void filterOpen() {
        setActiveSidebarBtn(btnMenuOpen);
        lblPageTitle.setText("Sắp diễn ra");
        activeFilter = this::filterOpen;
        runAsync(() -> {
            List<AuctionDto> filtered = api.getAllAuctions().stream()
                    .filter(a -> "OPEN".equals(resolveStatus(a))).toList();
            Platform.runLater(() -> loadData(filtered, filtered.size() + " phiên"));
        });
    }

    @FXML public void filterFinished() {
        setActiveSidebarBtn(btnMenuFinished);
        lblPageTitle.setText("Đã kết thúc");
        activeFilter = this::filterFinished;
        runAsync(() -> {
            List<AuctionDto> filtered = api.getAllAuctions().stream()
                    .filter(a -> {
                        String s = resolveStatus(a);
                        return "FINISHED".equals(s) || "PAID".equals(s) || "CANCELED".equals(s);
                    }).toList();
            Platform.runLater(() -> loadData(filtered, filtered.size() + " phiên"));
        });
    }

    @FXML public void showMyBids() {
        setActiveSidebarBtn(btnMenuMyBids);
        lblPageTitle.setText("Lịch sử bid của tôi");
        activeFilter = this::showMyBids;
        runAsync(() -> {
            List<Long> ids = api.getMyBids().stream()
                    .map(BidDto::getAuctionId).distinct().toList();
            List<AuctionDto> auctions = ids.stream()
                    .map(id -> { try { return api.getAuction(id); } catch (Exception e) { return null; } })
                    .filter(a -> a != null).toList();
            Platform.runLater(() -> loadData(auctions, auctions.size() + " phiên"));
        });
    }

    @FXML public void showMySales() {
        setActiveSidebarBtn(btnMenuMySales);
        lblPageTitle.setText("Sản phẩm của tôi");
        activeFilter = this::showMySales;
        runAsync(() -> {
            List<AuctionDto> list = api.getMySales();
            Platform.runLater(() -> loadData(list, list.size() + " phiên"));
        });
    }

    private void loadData(List<AuctionDto> list, String countText) {
        // Sync property map với dữ liệu mới nhất từ server
        for (AuctionDto a : list) {
            if (a.getId() == null) continue;
            getPriceProp(a).set(a.getCurrentPrice());
            getBidCountProp(a).set(a.getTotalBids());
            getStatusProp(a).set(resolveStatus(a));
        }
        auctionData.setAll(list);
        lblCount.setText(countText);
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    @FXML public void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) { filterAll(); return; }
        setActiveSidebarBtn(null);
        lblPageTitle.setText("Kết quả tìm kiếm: \"" + keyword + "\"");
        activeFilter = this::handleSearch;
        runAsync(() -> {
            List<AuctionDto> list = api.searchAuctions(keyword);
            Platform.runLater(() -> loadData(list, list.size() + " kết quả"));
        });
    }

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
                case NEW_BID         -> handleNewBidEvent(event);
                case AUCTION_STARTED -> handleStatusChangeEvent(event, "RUNNING");
                case AUCTION_CLOSED  -> handleStatusChangeEvent(event, "FINISHED");
                case AUCTION_CANCELED -> handleStatusChangeEvent(event, "CANCELED");
                case AUCTION_CREATED -> addNewAuctionRow(event);
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
     * Xử lý NEW_BID event — cập nhật giá và số lượt bid tức thì.
     *
     * Cơ chế: set giá trị mới vào SimpleObjectProperty → TableView đang bind
     * vào property này tự động re-render cell giá và cell số bid ngay lập tức,
     * không cần gọi tblAuctions.refresh().
     *
     * Chạy trên FX thread (đảm bảo bởi AuctionEventBus.publish).
     */
    private void handleNewBidEvent(AuctionEvent event) {
        Long id = event.getAuctionId();

        // Cập nhật price property → colPrice tự refresh
        SimpleObjectProperty<BigDecimal> priceProp = priceProps.get(id);
        if (priceProp != null) priceProp.set(event.getCurrentPrice());

        // Cập nhật bid count property → colBids tự refresh
        SimpleObjectProperty<Long> bidProp = bidCountProps.get(id);
        if (bidProp != null) bidProp.set(event.getTotalBids());

        // Sync lại AuctionDto để các thao tác khác (double-click, filter) nhận đúng giá
        auctionData.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setCurrentPrice(event.getCurrentPrice());
                    a.setTotalBids(event.getTotalBids());
                    if (event.getEndTime() != null) a.setEndTime(event.getEndTime());
                });
    }

    /**
     * Xử lý status change (STARTED / CLOSED / CANCELED).
     * Cập nhật status property → colStatus và row style tự refresh.
     */
    private void handleStatusChangeEvent(AuctionEvent event, String newStatus) {
        Long id = event.getAuctionId();

        SimpleObjectProperty<String> statusProp = statusProps.get(id);
        if (statusProp != null) statusProp.set(newStatus);

        auctionData.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setStatus(newStatus);
                    if (event.getEndTime() != null) a.setEndTime(event.getEndTime());
                });

        // Row style không bind vào property nên vẫn cần refresh để updateItem() chạy lại
        tblAuctions.refresh();
    }

    /**
     * Phiên mới được tạo → fetch đầy đủ từ API rồi thêm vào đầu list.
     */
    private void addNewAuctionRow(AuctionEvent event) {
        boolean exists = auctionData.stream()
                .anyMatch(a -> a.getId() != null && a.getId().equals(event.getAuctionId()));
        if (exists) return;

        Thread t = new Thread(() -> {
            try {
                AuctionDto dto = api.getAuction(event.getAuctionId());
                Platform.runLater(() -> {
                    if (!shouldShowInCurrentFilter(dto)) return;
                    // Init properties cho phiên mới trước khi add vào list
                    getPriceProp(dto).set(dto.getCurrentPrice());
                    getBidCountProp(dto).set(dto.getTotalBids());
                    getStatusProp(dto).set(resolveStatus(dto));
                    auctionData.add(0, dto);
                    lblCount.setText(auctionData.size() + " phiên");
                });
            } catch (Exception e) {
                System.err.println("[AuctionList] Cannot fetch new auction: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private boolean shouldShowInCurrentFilter(AuctionDto dto) {
        if (activeFilter == null) return true;
        String resolved = resolveStatus(dto);
        String title = lblPageTitle.getText();
        return switch (title) {
            case "Tất cả phiên đấu giá"  -> true;
            case "Đang diễn ra"           -> "RUNNING".equals(resolved);
            case "Sắp diễn ra"            -> "OPEN".equals(resolved);
            case "Đã kết thúc"            -> "FINISHED".equals(resolved)
                    || "PAID".equals(resolved) || "CANCELED".equals(resolved);
            case "Sản phẩm của tôi"       -> dto.getSeller() != null
                    && dto.getSeller().getUsername().equals(session.getUsername());
            default -> true;
        };
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
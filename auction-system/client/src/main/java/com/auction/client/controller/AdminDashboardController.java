package com.auction.client.controller;

import com.auction.client.model.ClientDto.*;
import com.auction.client.service.ApiService;
import com.auction.client.service.SessionManager;
import com.auction.client.util.FxUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.List;

public class AdminDashboardController {

    // User Table
    @FXML private TableView<UserDto> tblUsers;
    @FXML private TableColumn<UserDto, Long>       colUserId;
    @FXML private TableColumn<UserDto, String>     colUsername;
    @FXML private TableColumn<UserDto, String>     colEmail;
    @FXML private TableColumn<UserDto, String>     colRole;
    @FXML private TableColumn<UserDto, BigDecimal> colBalance;
    @FXML private TableColumn<UserDto, Boolean>    colStatus;
    @FXML private TableColumn<UserDto, Void>       colActions;
    @FXML private TextField txtSearchUser;

    // Auction Table
    @FXML private TableView<AuctionDto> tblAuctions;
    @FXML private TableColumn<AuctionDto, Long>       colAucId;
    @FXML private TableColumn<AuctionDto, String>     colAucName;
    @FXML private TableColumn<AuctionDto, String>     colAucSeller;
    @FXML private TableColumn<AuctionDto, BigDecimal> colAucPrice;
    @FXML private TableColumn<AuctionDto, String>     colAucStatus;
    @FXML private TableColumn<AuctionDto, String>     colAucEndTime;
    @FXML private TableColumn<AuctionDto, Void>       colAucActions;

    // Stats
    @FXML private Label lblTotalUsers;
    @FXML private Label lblRunningAuctions;
    @FXML private Label lblFinishedAuctions;
    @FXML private Label lblAdminName;
    @FXML private TabPane tabPane;

    private final ApiService api = ApiService.getInstance();
    private final ObservableList<UserDto>    userList    = FXCollections.observableArrayList();
    private final ObservableList<AuctionDto> auctionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.getInstance().getUsername());
        setupUserTable();
        setupAuctionTable();
        refreshAll();
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    getStyleClass().removeAll("label-accent", "label-danger");
                } else {
                    setText(active ? "● Hoạt động" : "✕ Bị khoá");
                    getStyleClass().removeAll("label-accent", "label-danger");
                    getStyleClass().add(active ? "label-accent" : "label-danger");
                }
            }
        });

        setupUserActionButtons();

        FilteredList<UserDto> filteredUsers = new FilteredList<>(userList, p -> true);
        txtSearchUser.textProperty().addListener((obs, oldVal, newVal) ->
                filteredUsers.setPredicate(user -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return user.getUsername().toLowerCase().contains(lower)
                            || user.getEmail().toLowerCase().contains(lower);
                }));
        tblUsers.setItems(filteredUsers);
    }

    private void setupUserActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnToggle  = new Button();
            private final Button btnAddMoney = new Button("+$");

            {
                btnToggle.setPrefWidth(72);
                btnAddMoney.getStyleClass().addAll("button", "btn-primary");
                btnAddMoney.setStyle("-fx-padding: 5 10; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserDto user = getTableView().getItems().get(getIndex());
                    btnToggle.setText(user.isActive() ? "Khoá" : "Mở");

                    btnToggle.getStyleClass().removeAll(
                            "button", "btn-danger", "btn-primary");
                    btnToggle.getStyleClass().addAll("button",
                            user.isActive() ? "btn-danger" : "btn-primary");
                    btnToggle.setStyle("-fx-padding: 5 10; -fx-font-size: 11px;");

                    btnToggle.setOnAction(e -> handleToggleUser(user));
                    btnAddMoney.setOnAction(e -> handleAddMoney(user));

                    HBox container = new HBox(6, btnToggle, btnAddMoney);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupAuctionTable() {
        colAucId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAucName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAucSeller.setCellValueFactory(column ->
                new javafx.beans.property.SimpleStringProperty(
                        column.getValue().getSeller().getUsername()));
        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAucEndTime.setCellValueFactory(column ->
                new javafx.beans.property.SimpleStringProperty(
                        column.getValue().getEndTime().toString()));

        setupAuctionActionButtons();
        tblAuctions.setItems(auctionList);
    }

    private void setupAuctionActionButtons() {
        colAucActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Xoá");
            private final Button btnPaid   = new Button("Paid");

            {
                btnDelete.getStyleClass().addAll("button", "btn-danger");
                btnDelete.setStyle("-fx-padding: 5 10; -fx-font-size: 11px;");
                btnPaid.getStyleClass().addAll("button", "btn-warn");
                btnPaid.setStyle("-fx-padding: 5 10; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AuctionDto auction = getTableView().getItems().get(getIndex());
                    btnPaid.setVisible("FINISHED".equals(auction.getStatus()));
                    btnPaid.setManaged("FINISHED".equals(auction.getStatus()));

                    btnDelete.setOnAction(e -> handleDeleteAuction(auction));
                    btnPaid.setOnAction(e -> handleMarkPaid(auction));

                    HBox container = new HBox(6, btnDelete, btnPaid);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    public void refreshAll() {
        refreshUsers();
        refreshAuctions();
    }

    @FXML
    public void refreshUsers() {
        new Thread(() -> {
            try {
                List<UserDto> users = api.getAllUsers();
                Platform.runLater(() -> {
                    userList.setAll(users);
                    lblTotalUsers.setText(String.valueOf(users.size()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError("Lỗi load user: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void refreshAuctions() {
        new Thread(() -> {
            try {
                List<AuctionDto> auctions = api.getAllAuctionsAdmin();
                Platform.runLater(() -> {
                    auctionList.setAll(auctions);
                    long running  = auctions.stream().filter(a -> "RUNNING".equals(a.getStatus())).count();
                    long finished = auctions.stream()
                            .filter(a -> "FINISHED".equals(a.getStatus()) || "PAID".equals(a.getStatus())).count();
                    lblRunningAuctions.setText(String.valueOf(running));
                    lblFinishedAuctions.setText(String.valueOf(finished));
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError("Lỗi load đấu giá: " + e.getMessage()));
            }
        }).start();
    }

    private void handleToggleUser(UserDto user) {
        String action = user.isActive() ? "khoá" : "mở khoá";
        if (!FxUtil.showConfirm("Bạn có chắc muốn " + action + " tài khoản " + user.getUsername() + "?")) return;

        new Thread(() -> {
            try {
                api.toggleUserActive(user.getId(), !user.isActive());
                Platform.runLater(() -> {
                    FxUtil.showInfo("Đã " + action + " thành công.");
                    refreshUsers();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    private void handleAddMoney(UserDto user) {
        FxUtil.showInputDialog("Cộng tiền", "Cộng tiền cho " + user.getUsername(), "1000000")
                .ifPresent(val -> {
                    try {
                        new BigDecimal(val);
                        FxUtil.showInfo("Chức năng này cần API Admin nạp tiền. Đang phát triển.");
                    } catch (Exception e) {
                        FxUtil.showError("Số tiền không hợp lệ.");
                    }
                });
    }

    private void handleDeleteAuction(AuctionDto auction) {
        if (!FxUtil.showConfirm("Bạn có chắc muốn xoá phiên đấu giá #" + auction.getId() + "?")) return;

        new Thread(() -> {
            try {
                api.deleteAuction(auction.getId());
                Platform.runLater(() -> {
                    FxUtil.showInfo("Đã xoá thành công.");
                    refreshAuctions();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    private void handleMarkPaid(AuctionDto auction) {
        if (!FxUtil.showConfirm("Đánh dấu phiên này đã thanh toán cho người bán?")) return;

        new Thread(() -> {
            try {
                api.markAuctionPaid(auction.getId());
                Platform.runLater(() -> {
                    FxUtil.showInfo("Đã xác nhận thanh toán.");
                    refreshAuctions();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtil.showError("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        FxUtil.switchScene(tabPane, "/fxml/login.fxml", "Đăng nhập");
    }
}

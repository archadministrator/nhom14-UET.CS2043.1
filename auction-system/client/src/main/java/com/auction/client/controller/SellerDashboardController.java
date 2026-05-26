package com.auction.client.controller;

import com.auction.client.model.ClientDto.AuctionDto;
import com.auction.client.service.ApiService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerDashboardController {

    @FXML private Label     lblFormTitle;
    @FXML private TextField txtName, txtStartPrice, txtIncrement, txtStartTime, txtEndTime, txtImageUrl;
    @FXML private TextArea  txtDesc;
    @FXML private Label     lblFormMsg;
    @FXML private Button    btnSave, btnEdit, btnDelete;

    @FXML private TableView<AuctionDto>          tblMyAuctions;
    @FXML private TableColumn<AuctionDto,String> colMyName, colMyPrice, colMyStatus, colMyBids;

    private final ObservableList<AuctionDto> myAuctions = FXCollections.observableArrayList();
    private final ApiService api = ApiService.getInstance();

    private static final DateTimeFormatter DTF_IN  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DTF_OUT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final NumberFormat CURRENCY =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private AuctionDto editingAuction = null;

    @FXML
    public void initialize() {
        setupTable();
        loadMyAuctions();
        prefillDefaultTimes();
    }

    private void setupTable() {
        colMyName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colMyPrice.setCellValueFactory(c ->
                new SimpleStringProperty(CURRENCY.format(c.getValue().getCurrentPrice().longValue()) + "₫"));
        colMyStatus.setCellValueFactory(c ->
                new SimpleStringProperty(translateStatus(c.getValue().getStatus())));
        colMyBids.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getTotalBids())));
        tblMyAuctions.setItems(myAuctions);
    }

    private void loadMyAuctions() {
        Thread t = new Thread(() -> {
            try {
                List<AuctionDto> list = api.getMySales();
                Platform.runLater(() -> myAuctions.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> showMsg("Lỗi tải dữ liệu: " + e.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void prefillDefaultTimes() {
        LocalDateTime now = LocalDateTime.now().plusMinutes(10);
        txtStartTime.setText(now.format(DTF_IN));
        txtEndTime.setText(now.plusDays(3).format(DTF_IN));
    }

    @FXML
    public void handleSelectAuction(javafx.scene.input.MouseEvent e) {
        AuctionDto selected = tblMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        btnEdit.setDisable(!"OPEN".equals(selected.getStatus()));
        btnDelete.setDisable("RUNNING".equals(selected.getStatus()));
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            fillFormForEdit(selected);
        }
    }

    @FXML
    public void handleNew() {
        editingAuction = null;
        lblFormTitle.setText("Tạo phiên đấu giá mới");
        btnSave.setText("Tạo phiên");
        clearForm();
        prefillDefaultTimes();
    }

    @FXML
    public void handleEdit() {
        AuctionDto selected = tblMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) { FxUtil.showError("Vui lòng chọn 1 phiên."); return; }
        fillFormForEdit(selected);
    }

    private void fillFormForEdit(AuctionDto a) {
        editingAuction = a;
        lblFormTitle.setText("Chỉnh sửa: " + a.getName());
        btnSave.setText("Lưu thay đổi");
        txtName.setText(a.getName());
        txtDesc.setText(a.getDescription() != null ? a.getDescription() : "");
        txtStartPrice.setText(a.getStartPrice().toPlainString());
        txtIncrement.setText(a.getMinIncrement().toPlainString());
        txtStartTime.setText(a.getStartTime() != null ? a.getStartTime().format(DTF_IN) : "");
        txtEndTime.setText(a.getEndTime() != null ? a.getEndTime().format(DTF_IN) : "");
        txtImageUrl.setText(a.getImageUrl() != null ? a.getImageUrl() : "");
    }

    @FXML
    public void handleSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) { showMsg("Tên sản phẩm không được để trống.", false); return; }

        BigDecimal startPrice, increment;
        try {
            startPrice = new BigDecimal(txtStartPrice.getText().trim().replace(",", ""));
            increment  = new BigDecimal(txtIncrement.getText().trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            showMsg("Giá khởi điểm hoặc mức tăng không hợp lệ.", false);
            return;
        }

        LocalDateTime startTime, endTime;
        try {
            startTime = LocalDateTime.parse(txtStartTime.getText().trim(), DTF_IN);
            endTime   = LocalDateTime.parse(txtEndTime.getText().trim(), DTF_IN);
        } catch (Exception ex) {
            showMsg("Định dạng thời gian không đúng (yyyy-MM-ddTHH:mm).", false);
            return;
        }

        if (!endTime.isAfter(startTime.plusMinutes(4))) {
            showMsg("Thời gian kết thúc phải sau bắt đầu ít nhất 5 phút.", false);
            return;
        }

        if (!startTime.isAfter(LocalDateTime.now())) {
            showMsg("Thời gian bắt đầu phải ở trong tương lai.", false);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name",         name);
        data.put("description",  txtDesc.getText().trim());
        data.put("startPrice",   startPrice);
        data.put("minIncrement", increment);
        data.put("startTime",    startTime.format(DTF_OUT));
        data.put("endTime",      endTime.format(DTF_OUT));
        data.put("imageUrl",     txtImageUrl.getText().trim());

        btnSave.setDisable(true);
        Thread t = new Thread(() -> {
            try {
                if (editingAuction == null) {
                    api.createAuction(data);
                    Platform.runLater(() -> {
                        showMsg("Tạo phiên đấu giá thành công!", true);
                        clearForm();
                        prefillDefaultTimes();
                        loadMyAuctions();
                    });
                } else {
                    api.updateAuction(editingAuction.getId(), data);
                    Platform.runLater(() -> {
                        showMsg("Cập nhật thành công!", true);
                        loadMyAuctions();
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showMsg("Lỗi: " + ex.getMessage(), false));
            } finally {
                Platform.runLater(() -> btnSave.setDisable(false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleDelete() {
        AuctionDto selected = tblMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) { FxUtil.showError("Vui lòng chọn 1 phiên."); return; }
        if (!FxUtil.showConfirm("Xóa phiên \"" + selected.getName() + "\"?")) return;

        Thread t = new Thread(() -> {
            try {
                api.deleteAuction(selected.getId());
                Platform.runLater(() -> {
                    showMsg("Đã xóa phiên đấu giá.", true);
                    loadMyAuctions();
                    clearForm();
                    editingAuction = null;
                    btnEdit.setDisable(true);
                    btnDelete.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg("Lỗi xóa: " + e.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML public void handleClearForm() { handleNew(); }

    @FXML
    public void handleBack() {
        FxUtil.switchScene(tblMyAuctions, "/fxml/auction-list.fxml", "Danh sách đấu giá");
    }

    private void clearForm() {
        txtName.clear(); txtDesc.clear(); txtStartPrice.clear();
        txtIncrement.clear(); txtStartTime.clear(); txtEndTime.clear(); txtImageUrl.clear();
        hideMsg();
    }

    /**
     * Show form message using CSS classes — no inline style strings.
     * success → label-accent (green), failure → label-danger (red)
     */
    private void showMsg(String msg, boolean success) {
        lblFormMsg.setText(msg);
        lblFormMsg.getStyleClass().removeAll("label-accent", "label-danger", "label");
        lblFormMsg.getStyleClass().add(success ? "label-accent" : "label-danger");
        lblFormMsg.setVisible(true);
        lblFormMsg.setManaged(true);
    }

    private void hideMsg() {
        lblFormMsg.setVisible(false);
        lblFormMsg.setManaged(false);
    }

    private String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "OPEN"     -> "○ Sắp bắt đầu";
            case "RUNNING"  -> "● Đang diễn ra";
            case "FINISHED" -> "✓ Đã kết thúc";
            case "PAID"     -> "✓ Đã thanh toán";
            case "CANCELED" -> "✕ Đã hủy";
            default         -> s;
        };
    }
}

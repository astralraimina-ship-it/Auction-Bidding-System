package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.network.ClientManager;
import com.auction.ui.dialogs.TransactionController;
import com.auction.util.NavigationService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

public class BidderController {
    // Các cột cho bảng Sàn đấu giá (Đang mở)
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails, colSeller;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft;

    // Các cột cho bảng Sản phẩm đã thắng
    @FXML private TableView<Item> tableWonItems;
    @FXML private TableColumn<Item, String> colWonName, colWonSeller;
    @FXML private TableColumn<Item, Double> colWonPrice;
    @FXML private TableColumn<Item, Timestamp> colWonDate;

    @FXML private Label lblBalance;
    @FXML private Button btnRefresh;

    private ItemDAO itemDAO = new ItemDAO();
    private SellerDAO sellerDAO = new SellerDAO();
    private String username;
    private int userId;

    @FXML
    public void initialize() {
        setupAuctionColumns();
        setupWonColumns();
        setupRowFactory();

        // ĐÃ SỬA: Lắng nghe và bóc tách các phản hồi PAY_SUCCESS / PAY_FAILED từ Server
        ClientManager.getInstance().setUpdateListener(signal -> {
            Platform.runLater(() -> {
                if (signal.equals("Refresh")){
                    refreshAll();
                }
                else if (signal.equals("PAY_SUCCESS")) {
                    showSimpleAlert("Thành công", "Chúc mừng! Bạn đã hoàn tất thanh toán đơn hàng thành công.", Alert.AlertType.INFORMATION);
                    refreshAll(); // Tự động load lại số dư và ẩn item đã mua
                }
                else if (signal.startsWith("PAY_FAILED")) {
                    String[] parts = signal.split(";");
                    String reason = parts.length > 1 ? parts[1] : "Lỗi xử lý giao dịch hoặc tài khoản không đủ tiền.";
                    showSimpleAlert("Thanh toán thất bại", reason, Alert.AlertType.ERROR);
                }
            });
        });
    }

    private void setupAuctionColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        setupTimeLeftColumn();

        // ĐỊNH DẠNG LẠI TIỀN TỆ (XÓA SỐ MŨ E) CHO BẢNG SÀN ĐẤU GIÁ
        setupPriceColumnFormat(colStartPrice);
        setupPriceColumnFormat(colBinPrice);
    }

    private void setupWonColumns() {
        colWonName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWonSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colWonPrice.setCellValueFactory(new PropertyValueFactory<>("winPrice"));
        colWonDate.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // ĐỊNH DẠNG LẠI TIỀN TỆ (XÓA SỐ MŨ E) CHO BẢNG SẢN PHẨM ĐÃ THẮNG
        setupPriceColumnFormat(colWonPrice);

        // ĐÃ BỔ SUNG: Đếm ngược thời gian giới hạn thanh toán 24h
        colWonDate.setCellFactory(column -> new TableCell<Item, Timestamp>() {
            @Override
            protected void updateItem(Timestamp endTime, boolean empty) {
                super.updateItem(endTime, empty);
                if (empty || endTime == null) {
                    setText(null);
                    setStyle("");
                } else {
                    LocalDateTime endAuctionTime = endTime.toLocalDateTime();
                    LocalDateTime paymentDeadline = endAuctionTime.plusDays(1); // Thời hạn 24 giờ
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(paymentDeadline)) {
                        setText("Quá hạn thanh toán!");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Màu đỏ quá hạn
                    } else {
                        Duration d = Duration.between(now, paymentDeadline);
                        long hours = d.toHours();
                        long mins = d.toMinutesPart();
                        long secs = d.toSecondsPart();

                        setText(String.format("Còn %02dh:%02dm:%02ds", hours, mins, secs));
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Màu cam tiến độ
                    }
                }
            }
        });
    }

    // Hàm phụ trách ép định dạng tiền Double -> dạng chuỗi phân tách dấu phẩy (%,.0f VNĐ)
    private void setupPriceColumnFormat(TableColumn<Item, Double> column) {
        column.setCellFactory(col -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                }
            }
        });
    }

    private void setupTimeLeftColumn() {
        colTimeLeft.setCellFactory(column -> new TableCell<Item, Timestamp>() {
            @Override
            protected void updateItem(Timestamp endTime, boolean empty) {
                super.updateItem(endTime, empty);
                if (empty || endTime == null) {
                    setText(null);
                    setStyle("");
                } else {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime end = endTime.toLocalDateTime();
                    if (now.isAfter(end)) {
                        setText("Đã kết thúc");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        Duration d = Duration.between(now, end);
                        long days = d.toDays();
                        if (days > 0) setText(days + " ngày " + d.toHoursPart() + "h");
                        else setText(String.format("%02dh:%02dm:%02ds", d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupRowFactory() {
        tableItems.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openAuctionRoom(row.getItem());
                }
            });
            return row;
        });
    }

    private void openAuctionRoom(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/bidder_auction_room.fxml"));
            Parent root = loader.load();

            BidderAuctionRoomController controller = loader.getController();
            controller.setUserId(this.userId);
            controller.initData(item);

            Stage stage = new Stage();
            stage.setTitle("Phòng đấu giá: " + item.getName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            refreshAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBidderInfo(int id, String username) {
        this.userId = id;
        this.username = username;
        refreshAll();
    }

    @FXML
    public void refreshAll() {
        if (btnRefresh != null) btnRefresh.setDisable(true);
        new Thread(() -> {
            try {
                double balance = (username != null) ? sellerDAO.getUserBalance(username) : 0;
                ObservableList<Item> openItems = itemDAO.getAllOpenItems();
                ObservableList<Item> wonItems = itemDAO.getWonItems(this.userId);

                Platform.runLater(() -> {
                    if (lblBalance != null) lblBalance.setText(String.format("%,.0f VNĐ", balance));
                    if (tableItems != null) tableItems.setItems(openItems);
                    if (tableWonItems != null) tableWonItems.setItems(wonItems);
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> { if (btnRefresh != null) btnRefresh.setDisable(false); });
            }
        }).start();
    }

    @FXML
    private void handlePayment() {
        Item selected = tableWonItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSimpleAlert("Cảnh báo", "Vui lòng chọn món đồ đã thắng để thanh toán!", Alert.AlertType.WARNING);
            return;
        }

        // 1. Kiểm tra quá hạn thanh toán
        if (selected.getEndTime() != null) {
            LocalDateTime endAuctionTime = selected.getEndTime().toLocalDateTime();
            LocalDateTime paymentDeadline = endAuctionTime.plusDays(1);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(paymentDeadline)) {
                showSimpleAlert("Từ chối thanh toán", "Sản phẩm này đã quá hạn thanh toán 24h!", Alert.AlertType.ERROR);
                return;
            }
        }

        // 2. Lấy giá trị số thuần túy, TUYỆT ĐỐI KHÔNG đọc từ chuỗi hiển thị trên bảng
        try {
            // Lấy giá thực tế từ object (ví dụ: 100000.0)
            double finalPrice = selected.getCurrentPrice();

            // Chuẩn hóa số dư hiển thị: xóa hết ký tự lạ, chỉ giữ lại số từ 0-9
            String cleanBalanceStr = lblBalance.getText().replaceAll("[^0-9]", "");
            double currentBalance = Double.parseDouble(cleanBalanceStr);

            if (currentBalance < finalPrice) {
                showSimpleAlert("Thanh toán thất bại", "Tài khoản của bạn không đủ số dư!", Alert.AlertType.ERROR);
                return;
            }

            // 3. Gửi lệnh lên Server (truyền số double chuẩn format số máy tính)
            // Sử dụng String.format với Locale.US để đảm bảo dấu phân tách phần thập phân luôn là dấu chấm, không bị đổi thành dấu phẩy
            String command = String.format(java.util.Locale.US, "PAY;%d;%d;%.2f", selected.getId(), this.userId, finalPrice);
            System.out.println("Gửi yêu cầu thanh toán hợp lệ: " + command);
            ClientManager.getInstance().sendCommand(command);

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Lỗi hệ thống", "Không thể xử lý dữ liệu số tiền!", Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleOpenDeposit() { openTransactionWindow("DEPOSIT"); }
    @FXML private void handleOpenWithdraw() { openTransactionWindow("WITHDRAW"); }

    private void openTransactionWindow(String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/deposit_view.fxml"));
            Parent root = loader.load();
            TransactionController controller = loader.getController();
            controller.setUsername(this.username);
            controller.setMode(mode);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // BỔ SUNG PHƯƠNG THỨC ĐĂNG XUẤT THEO ĐÚNG FXML
    @FXML
    private void handleLogout() {
        if (lblBalance != null && lblBalance.getScene() != null) {
            Stage stage = (Stage) lblBalance.getScene().getWindow();
            NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
        }
    }

    // Hàm phụ trợ dùng chung để hiển thị Alert nhanh gọn, không vỡ layout
    private void showSimpleAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
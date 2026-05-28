package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.network.ClientManager;
import com.auction.ui.dialogs.TransactionController;
import com.auction.util.NavigationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import java.util.ArrayList;
import java.util.List;

public class BidderController implements ClientManager.UpdateListener {
    // --- Các cột cho bảng Sàn đấu giá (Đang mở) ---
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails, colSeller;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft;

    // --- Các cột cho bảng Sản phẩm đã thắng ---
    @FXML private TableView<Item> tableWonItems;
    @FXML private TableColumn<Item, String> colWonName, colWonSeller;
    @FXML private TableColumn<Item, Double> colWonPrice;
    @FXML private TableColumn<Item, Timestamp> colWonDate;

    // --- CÁC CỘT CHO BẢNG MỚI: Đang tham gia & Kết quả (Đã thua) ---
    @FXML private TableView<Item> tableParticipated;
    @FXML private TableColumn<Item, String> colPartName;
    @FXML private TableColumn<Item, Double> colPartPrice;
    @FXML private TableColumn<Item, String> colPartResult;

    @FXML private Label lblBalance;
    @FXML private Button btnRefresh;

    private ItemDAO itemDAO = new ItemDAO();
    private SellerDAO sellerDAO = new SellerDAO();
    private String username;
    private int userId;

    // Biến lưu trữ thể hiện để dùng cho việc Update Real-time
    private static BidderController instance;

    public static BidderController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this; // Gán instance

        setupAuctionColumns();
        setupWonColumns();
        setupParticipatedColumns(); // Bổ sung setup bảng mới
        setupRowFactory();

        ClientManager.getInstance().addUpdateListener(this);
    }

    @Override
    public void onUpdateReceived(String signal) {
        Platform.runLater(() -> {
            if (signal.equals("REFRESH")){
                refreshAll();
            }
            else if (signal.equals("PAY_SUCCESS")) {
                showSimpleAlert("Thành công", "Chúc mừng! Bạn đã hoàn tất thanh toán đơn hàng thành công.", Alert.AlertType.INFORMATION);
                refreshAll();
            }
            else if (signal.startsWith("PAY_FAILED")) {
                String[] parts = signal.split(";");
                String reason = parts.length > 1 ? parts[1] : "Lỗi xử lý giao dịch hoặc tài khoản không đủ tiền.";
                showSimpleAlert("Thanh toán thất bại", reason, Alert.AlertType.ERROR);
            }
            // 🔥 THÊM MỚI: Bắt gói dữ liệu Real-time cho bảng tham gia
            else if (signal.startsWith("PARTICIPATED_DATA")) {
                updateParticipatedTableData(signal);
            }
            // 🔥 THÊM MỚI: Nếu có ai đó vừa đặt giá, tự động ép xin lại bảng tham gia
            else if (signal.startsWith("BID_UPDATE") || signal.startsWith("AUCTION_CLOSED")) {
                if (userId > 0) {
                    ClientManager.getInstance().sendCommand("GET_PARTICIPATED_AUCTIONS;" + userId);
                }
            }
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

        setupPriceColumnFormat(colStartPrice);
        setupPriceColumnFormat(colBinPrice);
    }

    private void setupWonColumns() {
        colWonName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWonSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colWonPrice.setCellValueFactory(new PropertyValueFactory<>("winPrice"));
        colWonDate.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        setupPriceColumnFormat(colWonPrice);

        colWonDate.setCellFactory(column -> new TableCell<Item, Timestamp>() {
            @Override
            protected void updateItem(Timestamp endTime, boolean empty) {
                super.updateItem(endTime, empty);
                if (empty || endTime == null) {
                    setText(null);
                    setStyle("");
                } else {
                    LocalDateTime endAuctionTime = endTime.toLocalDateTime();
                    LocalDateTime paymentDeadline = endAuctionTime.plusDays(1);
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(paymentDeadline)) {
                        setText("Quá hạn thanh toán!");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        Duration d = Duration.between(now, paymentDeadline);
                        long hours = d.toHours();
                        long mins = d.toMinutesPart();
                        long secs = d.toSecondsPart();

                        setText(String.format("Còn %02dh:%02dm:%02ds", hours, mins, secs));
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    // --- 🔥 THÊM MỚI: Cài đặt cho bảng "Đang tham gia & Đã thua" ---
    private void setupParticipatedColumns() {
        colPartName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPartPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        setupPriceColumnFormat(colPartPrice); // Đồng bộ format tiền tệ

        colPartResult.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            if ("OPEN".equals(item.getStatus())) {
                return new javafx.beans.property.SimpleStringProperty("Đang tham gia 🔥");
            } else {
                return new javafx.beans.property.SimpleStringProperty("Đã thua ❌");
            }
        });
    }

    // --- ĐÃ CẬP NHẬT: Xử lý giải mã và khởi tạo Item đầy đủ step và binPrice ---
    public void updateParticipatedTableData(String rawData) {
        String[] parts = rawData.split(";");
        if (parts.length < 2 || parts[1].isEmpty()) {
            tableParticipated.setItems(FXCollections.observableArrayList());
            return;
        }

        String[] rows = parts[1].split("\\|");
        List<Item> list = new ArrayList<>();

        for (String row : rows) {
            String[] fields = row.split(",");
            // ĐÃ SỬA: Đổi từ 4 thành 6 để đọc đủ 6 tham số gửi từ Server
            if (fields.length >= 6) {
                // 1. Tạo Map chứa dữ liệu chung (Common Data)
                java.util.Map<String, Object> commonData = new java.util.HashMap<>();
                commonData.put("id", Integer.parseInt(fields[0]));
                commonData.put("name", fields[1]);
                commonData.put("currentPrice", Double.parseDouble(fields[2]));
                commonData.put("step", Double.parseDouble(fields[3]));     // Đã nạp giá trị Bước giá (Step)
                commonData.put("binPrice", Double.parseDouble(fields[4])); // Đã nạp Giá Mua Đứt (BIN)
                commonData.put("status", fields[5]);                       // Đẩy status lên vị trí số 5

                // Các trường còn lại để mặc định
                commonData.put("description", "");
                commonData.put("startPrice", 0.0);
                commonData.put("winPrice", 0.0);
                commonData.put("sellerName", "Unknown");
                commonData.put("endTime", null);
                commonData.put("paymentStatus", "PENDING");

                // 2. Tạo Map chứa dữ liệu riêng (Specific Data) trống
                java.util.Map<String, Object> specificData = new java.util.HashMap<>();

                // 3. Gọi Factory để tạo đối tượng Item cụ thể (mặc định loại "OTHER")
                Item item = com.auction.common.item.ItemFactory.createItem("OTHER", commonData, specificData);

                list.add(item);
            }
        }
        tableParticipated.setItems(FXCollections.observableArrayList(list));
    }

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

        // Hỗ trợ đúp chuột vào bảng Đang Tham Gia để mở lại phòng đấu giá (nếu phòng đang OPEN)
        tableParticipated.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Item item = row.getItem();
                    if ("OPEN".equals(item.getStatus())) {
                        openAuctionRoom(item);
                    } else {
                        showSimpleAlert("Thông báo", "Phòng đấu giá này đã kết thúc!", Alert.AlertType.INFORMATION);
                    }
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
            stage.setOnCloseRequest(event -> {
                System.out.println(">>> Đóng phòng đấu giá, hủy đăng ký Listener.");
                ClientManager.getInstance().removeUpdateListener(controller);
            });
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

                // Gọi Server để nạp dữ liệu cho Bảng Đang tham gia
                if (userId > 0) {
                    ClientManager.getInstance().sendCommand("GET_PARTICIPATED_AUCTIONS;" + userId);
                }
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

        if (selected.getEndTime() != null) {
            LocalDateTime endAuctionTime = selected.getEndTime().toLocalDateTime();
            LocalDateTime paymentDeadline = endAuctionTime.plusDays(1);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(paymentDeadline)) {
                showSimpleAlert("Từ chối thanh toán", "Sản phẩm này đã quá hạn thanh toán 24h!", Alert.AlertType.ERROR);
                return;
            }
        }

        try {
            double finalPrice = selected.getCurrentPrice();
            String cleanBalanceStr = lblBalance.getText().replaceAll("[^0-9]", "");
            double currentBalance = Double.parseDouble(cleanBalanceStr);

            if (currentBalance < finalPrice) {
                showSimpleAlert("Thanh toán thất bại", "Tài khoản của bạn không đủ số dư!", Alert.AlertType.ERROR);
                return;
            }

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

    @FXML
    private void handleLogout() {
        if (lblBalance != null && lblBalance.getScene() != null) {
            instance = null; // Xóa instance khi đăng xuất
            ClientManager.getInstance().removeUpdateListener(this);
            Stage stage = (Stage) lblBalance.getScene().getWindow();
            NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
        }
    }

    private void showSimpleAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
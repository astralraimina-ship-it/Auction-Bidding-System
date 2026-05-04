package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
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
    }

    private void setupAuctionColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        setupTimeLeftColumn();
    }

    private void setupWonColumns() {
        colWonName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWonSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colWonPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colWonDate.setCellValueFactory(new PropertyValueFactory<>("endTime"));
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

            // --- DÒNG SỬA LỖI QUAN TRỌNG NHẤT Ở ĐÂY ---
            controller.setUserId(this.userId); // Truyền userId thực tế sang để lưu bid
            controller.initData(item);

            Stage stage = new Stage();
            stage.setTitle("Phòng đấu giá: " + item.getName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            refreshAll(); // Sau khi đóng phòng thì load lại cả 2 bảng
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

                // Load danh sách đồ đã thắng dựa trên userId thực tế
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
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng chọn món đồ đã thắng để thanh toán!");
            alert.show();
            return;
        }
        // Logic thanh toán: Update tiền... (Tạm thời in ra log)
        System.out.println("Thanh toán cho món đồ ID: " + selected.getId() + " bởi User ID: " + this.userId);
        refreshAll();
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
        Stage stage = (Stage) lblBalance.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
    }
}
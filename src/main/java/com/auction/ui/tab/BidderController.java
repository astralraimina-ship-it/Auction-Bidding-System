package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.ui.dialogs.TransactionController;
import com.auction.util.NavigationService;
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
    @FXML private Label lblBalance;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails, colSeller;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft;

    private ItemDAO itemDAO = new ItemDAO();
    private SellerDAO sellerDAO = new SellerDAO(); // Dùng chung để lấy balance
    private String username;
    private int userId;

    @FXML
    public void initialize() {
        // 1. Map dữ liệu vào cột
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));

        // Cột người bán - Đảm bảo ItemDAO của bạn có put("sellerName", ...)
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        // 2. Định dạng đếm ngược thời gian
        setupTimeLeftColumn();

        refreshTable();
    }

    private void setupTimeLeftColumn() {
        colTimeLeft.setCellValueFactory(new PropertyValueFactory<>("endTime"));
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
                        if (days > 0) {
                            setText(days + " ngày " + d.toHoursPart() + "h");
                        } else {
                            setText(String.format("%02dh:%02dm:%02ds", d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                        }
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    public void setBidderInfo(int id, String username) {
        this.userId = id;
        this.username = username;
        refreshBalance();
        refreshTable();
    }

    public void refreshBalance() {
        if (lblBalance != null && username != null) {
            double balance = sellerDAO.getUserBalance(username);
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
        }
    }

    @FXML
    public void refreshTable() {
        if (tableItems != null) {
            // Lấy tất cả sản phẩm đang trong trạng thái OPEN từ DB
            tableItems.setItems(itemDAO.getAllOpenItems());
        }
    }

    @FXML
    private void handleOpenDeposit() { openTransactionWindow("DEPOSIT"); }

    @FXML
    private void handleOpenWithdraw() { openTransactionWindow("WITHDRAW"); }

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
            refreshBalance();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setBidderInfo(String username) {
        this.username = username;
        // Vì không có ID nên userId sẽ mặc định là 0 hoặc lấy từ DB sau
        refreshBalance();
        refreshTable();
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) tableItems.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
    }
}
package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.ui.dialogs.AddItemController;
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

public class SellerController {
    @FXML private Label lblBalance;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft;
    @FXML private Button btnRefresh; // Nút làm mới mới thêm

    private SellerDAO sellerDAO = new SellerDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private int sellerId;
    private String username;

    @FXML
    public void initialize() {
        setupColumns();
        setupTimeLeftColumn();
    }

    private void setupColumns() {
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCategory != null) colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colDetails != null) colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colStartPrice != null) colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        if (colBinPrice != null) colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));
    }

    private void setupTimeLeftColumn() {
        if (colTimeLeft != null) {
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
                                setText(String.format("%d ngày %02dh:%02dm", days, d.toHoursPart(), d.toMinutesPart()));
                            } else {
                                setText(String.format("%02d giờ %02d phút", d.toHoursPart(), d.toMinutesPart()));
                            }
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    public void setSellerInfo(int id, String username) {
        this.sellerId = id;
        this.username = username;
        refreshAll();
    }

    @FXML
    public void refreshAll() {
        // Vô hiệu hóa nút để tránh spam click khi đang load
        if (btnRefresh != null) btnRefresh.setDisable(true);

        new Thread(() -> {
            try {
                // 1. Lấy dữ liệu từ DB (Chạy ngầm)
                double balance = (username != null) ? sellerDAO.getUserBalance(username) : 0;
                ObservableList<Item> items = itemDAO.getItemsBySeller(this.sellerId);

                // 2. Cập nhật lên UI (Phải chạy trong Platform.runLater)
                Platform.runLater(() -> {
                    if (lblBalance != null) {
                        lblBalance.setText(String.format("%,.0f VNĐ", balance));
                    }
                    if (tableItems != null) {
                        tableItems.setItems(items);
                    }
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
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
            stage.setTitle(mode.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/add_item.fxml"));
            Parent root = loader.load();
            AddItemController controller = loader.getController();
            controller.setUserId(this.sellerId);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Đăng sản phẩm mới");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        if (tableItems != null && tableItems.getScene() != null) {
            Stage stage = (Stage) tableItems.getScene().getWindow();
            NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
        }
    }
}
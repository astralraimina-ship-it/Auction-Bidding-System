package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.ui.dialogs.AddItemController;
import com.auction.ui.dialogs.TransactionController;
import com.auction.util.NavigationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colDetails;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft; // Đổi sang Timestamp để xử lý logic

    private SellerDAO sellerDAO = new SellerDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private int sellerId;
    private String username;

    @FXML
    public void initialize() {
        // 1. Map dữ liệu cơ bản
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCategory != null) colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colDetails != null) colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colStartPrice != null) colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        if (colBinPrice != null) colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));

        // 2. Logic tính "Thời gian còn lại" (Countdown)
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
                            long hours = d.toHoursPart();
                            long minutes = d.toMinutesPart();

                            if (days > 0) {
                                setText(String.format("%d ngày %02dh:%02dm", days, hours, minutes));
                            } else {
                                setText(String.format("%02d giờ %02d phút", hours, minutes));
                            }
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
        refreshTable();
    }

    public void setSellerInfo(int id, String username) {
        this.sellerId = id;
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
            tableItems.setItems(itemDAO.getItemsBySeller(this.sellerId));
        }
    }

    @FXML
    private void handleOpenDeposit() {
        openTransactionWindow("DEPOSIT");
    }

    @FXML
    private void handleOpenWithdraw() {
        openTransactionWindow("WITHDRAW");
    }

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

            refreshBalance();
        } catch (IOException e) {
            System.err.println("Lỗi nạp deposit_view.fxml: " + e.getMessage());
        }
    }

    @FXML
    public void onAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/add_item.fxml"));
            Parent root = loader.load();

            // 1. Lấy đúng instance của AddItemController vừa được load
            AddItemController controller = loader.getController();

            // 2. TRUYỀN ID CỦA NGƯỜI BÁN SANG (Biến này đã có sẵn trong SellerController của Long)
            controller.setUserId(this.sellerId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Đăng sản phẩm mới");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshTable(); // Cập nhật lại bảng sau khi đăng xong
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
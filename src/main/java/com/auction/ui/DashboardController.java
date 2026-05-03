package com.auction.ui;

import com.auction.common.item.Item;
import com.auction.common.user.User;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;
import com.auction.transaction.Transaction;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {

    // --- Biến UI cho Sản phẩm (Bảng quản lý sản phẩm) ---
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails;
    @FXML private TableColumn<Item, Double> colBinPrice;
    @FXML private Label lblBalance;

    // --- Admin: Quản lý User ---
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML private TableColumn<User, Double> colUserBalance;

    // --- Admin: Quản lý Giao dịch ---
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colTransUser, colTransType;
    @FXML private TableColumn<Transaction, Double> colTransAmount, colTransNet;

    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private int userId;
    private String currentUsername;

    @FXML
    public void initialize() {
        // Kiểm tra từng bảng để tránh lỗi khi dùng chung Controller cho nhiều màn hình FXML
        if (tableItems != null) setupItemTable();
        if (tableUsers != null) setupAdminTable();
        if (tableTransactions != null) setupTransactionTable();
    }

    // --- LOGIC SELLER/ADMIN: BẢNG SẢN PHẨM ---
    private void setupItemTable() {
        // FIX: Đảm bảo PropertyValueFactory khớp với tên các hàm Getter trong class Item/Other/Vehicle
        if (colName != null) {
            colName.setCellValueFactory(new PropertyValueFactory<>("name")); // Gọi getName()
        }
        if (colCategory != null) {
            colCategory.setCellValueFactory(new PropertyValueFactory<>("category")); // Gọi getCategory()
        }
        if (colDetails != null) {
            // FIX QUAN TRỌNG: Phải là "itemDetails" để JavaFX tự gọi hàm getItemDetails()
            colDetails.setCellValueFactory(new PropertyValueFactory<>("itemDetails"));
        }
        if (colBinPrice != null) {
            colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice")); // Gọi getBinPrice()
        }

        refreshTable();
    }

    // --- LOGIC ADMIN: QUẢN LÝ NGƯỜI DÙNG & GIAO DỊCH ---
    private void setupAdminTable() {
        if (colUsername != null) colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colRole != null) colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colUserBalance != null) colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        if (tableUsers != null) tableUsers.setItems(userDAO.getAllUsers());
    }

    private void setupTransactionTable() {
        if (colTransUser != null) colTransUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colTransType != null) colTransType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (colTransAmount != null) colTransAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        if (colTransNet != null) colTransNet.setCellValueFactory(new PropertyValueFactory<>("netAmount"));

        loadTransactionData();
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---

    @FXML
    private void onAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/add_item.fxml"));
            Parent root = loader.load();
            AddItemController controller = loader.getController();
            if (controller != null) controller.setUserId(this.userId);

            Stage stage = new Stage();
            stage.setTitle("Đăng sản phẩm mới - UET Auction");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshTable();
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể mở màn hình đăng sản phẩm. Hãy kiểm tra đường dẫn file add_item.fxml!");
        }
    }

    private void refreshTable() {
        if (tableItems != null) {
            // Lấy toàn bộ danh sách từ DB và đổ vào Table
            tableItems.setItems(itemDAO.getAllItems());
            tableItems.refresh();
        }
    }

    @FXML
    private void handleApproveTrans() {
        Transaction selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoAlert("Thông báo", "Vui lòng chọn một giao dịch để duyệt!");
            return;
        }
        if (userDAO.approveTransaction(selected)) {
            loadTransactionData();
            showInfoAlert("Thành công", "Đã duyệt giao dịch.");
            refreshBalance();
        } else {
            showAlert("Lỗi", "Không thể duyệt giao dịch (có thể do số dư không đủ)!");
        }
    }

    @FXML
    private void handleRejectTrans() {
        Transaction selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoAlert("Thông báo", "Vui lòng chọn một giao dịch để từ chối!");
            return;
        }
        if (userDAO.rejectTransaction(selected.getId())) {
            loadTransactionData();
            showInfoAlert("Thành công", "Đã từ chối giao dịch.");
        }
    }

    private void loadTransactionData() {
        if (tableTransactions != null) {
            tableTransactions.setItems(userDAO.getPendingTransactions());
            tableTransactions.refresh();
        }
    }

    // --- QUẢN LÝ SESSION ---
    public void setUserData(int userId, String username) {
        this.userId = userId;
        this.currentUsername = username;
        refreshBalance();
    }

    public void refreshBalance() {
        if (lblBalance != null && currentUsername != null) {
            double balance = userDAO.getUserBalance(currentUsername);
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
        }
    }

    @FXML private void handleOpenDeposit() { openTransactionWindow("DEPOSIT"); }
    @FXML private void handleOpenWithdraw() { openTransactionWindow("WITHDRAW"); }

    private void openTransactionWindow(String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/deposit_view.fxml"));
            Parent root = loader.load();
            TransactionController controller = loader.getController();
            if (controller != null) {
                controller.setUsername(currentUsername);
                controller.setMode(mode);
            }
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshBalance();
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể mở cửa sổ giao dịch!");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/login.fxml"));
            Stage stage = (Stage) (lblBalance != null ? lblBalance.getScene().getWindow() :
                    (tableUsers != null ? tableUsers.getScene().getWindow() : tableItems.getScene().getWindow()));
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- TIỆN ÍCH THÔNG BÁO ---
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
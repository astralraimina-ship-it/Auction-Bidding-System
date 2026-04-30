package com.auction.ui;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
import com.auction.transaction.Transaction;
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

public class DashboardController {

    // --- Thành phần chung ---
    @FXML private Label lblBalance;

    // --- Thành phần Admin: Quản lý User ---
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML private TableColumn<User, Double> colUserBalance;

    // --- Thành phần Admin: Quản lý Giao dịch ---
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colTransUser, colTransType;
    @FXML private TableColumn<Transaction, Double> colTransAmount, colTransNet;

    private UserDAO userDAO = new UserDAO();
    private String currentUsername;

    @FXML
    public void initialize() {
        // 1. Setup bảng User
        if (tableUsers != null) {
            setupAdminTable();
        }

        // 2. Setup bảng Giao dịch
        if (tableTransactions != null) {
            setupTransactionTable();

            // DÒNG NÀY ĐỂ KIỂM TRA:
            // Nếu load xong mà console in ra > 0 thì là bảng có dữ liệu
            System.out.println("Khởi tạo Admin: Đang nạp dữ liệu giao dịch...");
            loadTransactionData();
        }
    }

    /**
     * Thiết lập các cột cho bảng User
     */
    private void setupAdminTable() {
        try {
            colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
            colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

            ObservableList<User> userList = userDAO.getAllUsers();
            if (userList != null) {
                tableUsers.setItems(userList);
            }
        } catch (Exception e) {
            System.err.println("Lỗi setup bảng Admin User: " + e.getMessage());
        }
    }

    /**
     * Thiết lập các cột cho bảng Giao dịch (Nạp/Rút)
     */
    private void setupTransactionTable() {
        try {
            colTransUser.setCellValueFactory(new PropertyValueFactory<>("username"));
            colTransType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colTransAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colTransNet.setCellValueFactory(new PropertyValueFactory<>("netAmount"));

            loadTransactionData();
            System.out.println("Đã setup bảng giao dịch thành công.");
        } catch (Exception e) {
            System.err.println("Lỗi setup bảng Giao dịch: " + e.getMessage());
        }
    }

    /**
     * Nạp dữ liệu thực tế từ Database vào bảng Giao dịch
     */
    private void loadTransactionData() {
        if (tableTransactions != null) {
            ObservableList<Transaction> pendingList = userDAO.getPendingTransactions();
            tableTransactions.setItems(pendingList);
            tableTransactions.refresh();
        }
    }

    // --- CÁC HÀM XỬ LÝ GIAO DỊCH DÀNH CHO ADMIN ---

    @FXML
    private void handleApproveTrans() {
        Transaction selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoAlert("Thông báo", "Vui lòng chọn một giao dịch để duyệt!");
            return;
        }

        if (userDAO.approveTransaction(selected)) {
            loadTransactionData(); // Cập nhật lại bảng ngay lập tức
            showInfoAlert("Thành công", "Đã duyệt giao dịch cho " + selected.getUsername());
            refreshBalance(); // Cập nhật lại số dư nếu admin cũng có ví
        } else {
            showAlert("Lỗi", "Không thể duyệt giao dịch. Vui lòng kiểm tra lại số dư người dùng!");
        }
    }

    @FXML
    private void handleRejectTrans() {
        Transaction selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoAlert("Thông báo", "Vui lòng chọn một giao dịch để từ chối!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn từ chối giao dịch của " + selected.getUsername() + "?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            if (userDAO.rejectTransaction(selected.getId())) {
                loadTransactionData();
                System.out.println("Đã từ chối giao dịch ID: " + selected.getId());
            } else {
                showAlert("Lỗi", "Không thể cập nhật trạng thái giao dịch!");
            }
        }
    }

    // --- CÁC HÀM DÀNH CHO USER (BIDDER/SELLER) ---

    public void setUserData(String username) {
        this.currentUsername = username;
        refreshBalance();
    }

    public void refreshBalance() {
        if (lblBalance == null || currentUsername == null) return;
        double balance = userDAO.getUserBalance(currentUsername);
        lblBalance.setText(String.format("%,.0f VNĐ", balance));
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
            controller.setUsername(currentUsername);
            controller.setMode(mode);

            Stage stage = new Stage();
            stage.setTitle(mode.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshBalance();
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể mở cửa sổ giao dịch!");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (lblBalance != null ? lblBalance.getScene().getWindow() : tableUsers.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
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
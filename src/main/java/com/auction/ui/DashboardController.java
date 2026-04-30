package com.auction.ui;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
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

    // --- Thành phần chỉ có ở Admin Dashboard ---
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML private TableColumn<User, Double> colUserBalance;

    private UserDAO userDAO = new UserDAO();
    private String currentUsername;

    @FXML
    public void initialize() {
        // KIỂM TRA: Nếu có tableUsers thì mới setup bảng (Dành cho Admin)
        if (tableUsers != null) {
            setupAdminTable();
        }
    }

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
            System.err.println("Lỗi setup bảng Admin: " + e.getMessage());
        }
    }

    public void setUserData(String username) {
        this.currentUsername = username;
        refreshBalance();
    }

    public void refreshBalance() {
        if (lblBalance == null || currentUsername == null) return;

        double balance = userDAO.getUserBalance(currentUsername);

        if (balance == 0 && "admin".equalsIgnoreCase(currentUsername)) {
            lblBalance.setVisible(false);
        } else {
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
            lblBalance.setVisible(true);
        }
    }

    // --- LOGIC GIAO DỊCH (NẠP / RÚT) ---

    @FXML
    private void handleOpenDeposit() {
        openTransactionWindow("DEPOSIT");
    }

    @FXML
    private void handleOpenWithdraw() {
        openTransactionWindow("WITHDRAW");
    }

    /**
     * Hàm dùng chung để mở cửa sổ Nạp hoặc Rút tiền
     */
    private void openTransactionWindow(String mode) {
        try {
            // Nhớ đảm bảo file này có tên là deposit_view.fxml hoặc bạn đã đổi tên thành transaction_view.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/deposit_view.fxml"));
            Parent root = loader.load();

            TransactionController controller = loader.getController();
            controller.setUsername(currentUsername);
            controller.setMode(mode); // QUAN TRỌNG: Gọi hàm setMode để đổi giao diện Nạp/Rút

            Stage stage = new Stage();
            stage.setTitle(mode.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Cập nhật lại số dư sau khi đóng cửa sổ
            refreshBalance();
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể mở cửa sổ giao dịch!");
            e.printStackTrace();
        }
    }

    // --- LOGIC HỆ THỐNG ---

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) (lblBalance != null ? lblBalance.getScene().getWindow() : tableUsers.getScene().getWindow());

            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng nhập");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
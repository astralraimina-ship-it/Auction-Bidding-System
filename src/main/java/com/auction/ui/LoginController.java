package com.auction.ui;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String usernameInput = txtUsername.getText().trim();
        String passwordInput = txtPassword.getText().trim();

        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            showAlert("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        User loggedInUser = userDAO.authenticate(usernameInput, passwordInput);

        if (loggedInUser != null) {
            loadDashboard(loggedInUser);
        } else {
            showAlert("Đăng nhập thất bại", "Sai tên đăng nhập, mật khẩu hoặc tài khoản chưa được duyệt!");
        }
    }

    private void loadDashboard(User user) {
        try {
            // 1. Xác định file FXML dựa trên Role của User
            String fxmlPath = "";
            String role = user.getRole().toUpperCase();

            switch (role) {
                case "ADMIN":
                    fxmlPath = "/com/auction/ui/admin_dashboard.fxml";
                    break;
                case "SELLER":
                    fxmlPath = "/com/auction/ui/seller_dashboard.fxml";
                    break;
                default:
                    // Mặc định là khách hoặc người đấu giá (Bidder)
                    fxmlPath = "/com/auction/ui/main_dashboard.fxml";
                    break;
            }

            // 2. Load file giao diện tương ứng
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // 3. Truyền dữ liệu sang DashboardController
            DashboardController dashboard = loader.getController();
            dashboard.setUserData(user.getId(), user.getUsername());

            // 4. Cập nhật Stage (Cửa sổ)
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - " + role);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", "Không thể tải giao diện cho quyền: " + user.getRole());
        }
    }

    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/Register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng ký");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở màn hình đăng ký!");
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
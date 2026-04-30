package com.auction.ui;

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
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        String role = userDAO.authenticate(user, pass);

        if (role != null) {
            loadDashboard(role, user);
        } else {
            showAlert("Đăng nhập thất bại", "Sai tên đăng nhập, mật khẩu hoặc tài khoản chưa được duyệt!");
        }
    }

    @FXML
    private void goToRegister() {
        try {
            // Đảm bảo đường dẫn file FXML này là chính xác trong project của Long
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/Register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng ký");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở màn hình đăng ký!");
        }
    }

    private void loadDashboard(String role, String username) {
        try {
            String fxmlFile = switch (role.toUpperCase()) {
                case "ADMIN" -> "admin_dashboard.fxml";
                case "SELLER" -> "seller_dashboard.fxml";
                default -> "main_dashboard.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/" + fxmlFile));
            Parent root = loader.load();

            // Truyền dữ liệu sang DashboardController (Nhớ sửa hàm này bên kia thành 2 tham số nhé)
            DashboardController controller = loader.getController();
            controller.setUserData(username, role);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống đấu giá UET - " + role);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", "Không thể tải giao diện dashboard!");
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
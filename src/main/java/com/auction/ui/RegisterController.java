package com.auction.ui;

import com.auction.database.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole; // Trong FXML nhớ đặt ID là cbRole

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        String role = cbRole.getValue();

        if (user.isEmpty() || pass.isEmpty() || role == null) {
            showMsg("Lỗi", "Vui lòng nhập đủ thông tin!");
            return;
        }

        if (userDAO.register(user, pass, role)) {
            String msg = role.equals("ADMIN") ? "Đăng ký thành công! Chờ Admin khác duyệt." : "Đăng ký thành công!";
            showMsg("Thông báo", msg);
        } else {
            showMsg("Lỗi", "Đăng ký thất bại (Trùng tên đăng nhập).");
        }
    }
    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/login.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng nhập");
        } catch (IOException e) {
            System.err.println("Không thể quay lại màn hình Login: " + e.getMessage());
        }
    }

    private void showMsg(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
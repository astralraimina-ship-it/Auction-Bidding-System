package com.auction.client;

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

    // Hàm này phải trùng tên với cái Long vừa gõ trong Scene Builder
    @FXML
    public void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {
            try {
                // Chuyển sang Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/MainDashboard.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) txtUsername.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Hệ thống đấu giá UET - Dashboard");
                stage.show();

                System.out.println("✅ Đăng nhập thành công, đang chuyển cảnh...");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không tìm thấy file Dashboard! Kiểm tra lại đường dẫn.");
            }
        } else {
            showAlert("Thất bại", "Sai tài khoản hoặc mật khẩu!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
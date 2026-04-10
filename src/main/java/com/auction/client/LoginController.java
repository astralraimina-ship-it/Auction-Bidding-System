package com.auction.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginController {

    // 1. Khai báo các biến khớp với fx:id bên FXML
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // 2. Hàm xử lý khi nhấn nút (tên hàm phải khớp với onAction bên FXML)
    @FXML
    public void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // 3. Logic kiểm tra (Tạm thời kiểm tra cứng để hiểu luồng)
        // This is a test profile
        if (user.equals("admin") && pass.equals("123")) {
            showNotification("Thành công", "Chào mừng đã đăng nhập!");
            // Sau này đoạn này sẽ là code để chuyển sang màn hình đấu giá
        } else {
            showNotification("Thất bại", "Sai tên đăng nhập hoặc mật khẩu rồi!");
        }
    }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
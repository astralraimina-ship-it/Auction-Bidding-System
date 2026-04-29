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
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        String role = userDAO.authenticate(user, pass);

        if (role != null) {
            // Đăng nhập thành công -> Chuyển màn hình
            loadDashboard(role, user);
        } else {
            // Đăng nhập thất bại -> HIỆN THÔNG BÁO Ở ĐÂY
            showAlert("Đăng nhập thất bại", "Sai tên đăng nhập, mật khẩu hoặc tài khoản chưa được duyệt!");
        }
    }

    @FXML
    private void goToRegister() {
        try {
            // Chuyển sang màn hình Register.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/Register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng ký");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDashboard(String role, String username) {
        try {
            String fxmlFile = "";
            switch (role.toUpperCase()) {
                case "ADMIN": fxmlFile = "admin_dashboard.fxml"; break;
                case "SELLER": fxmlFile = "seller_dashboard.fxml"; break;
                case "BIDDER": fxmlFile = "main_dashboard.fxml"; break;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/" + fxmlFile));
            Parent root = loader.load();

            // Lấy controller của dashboard để truyền username sang
            DashboardController controller = loader.getController();
            controller.setUserData(username,role);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR); // Loại ERROR sẽ có icon dấu X đỏ
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
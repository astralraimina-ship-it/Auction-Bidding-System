package com.auction.ui;

import com.auction.database.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        // authenticate trả về role nếu thành công, null nếu thất bại hoặc chưa APPROVED
        String role = userDAO.authenticate(user, pass);

        if (role != null) {
            loadDashboard(role, user);
        } else {
            System.out.println("Đăng nhập thất bại: Sai thông tin hoặc tài khoản chờ duyệt!");
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
            controller.setUserData(username);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
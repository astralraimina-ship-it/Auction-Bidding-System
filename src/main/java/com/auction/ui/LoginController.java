package com.auction.ui;

import com.auction.database.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        String role = userDAO.authenticate(user, pass); // Trả về ADMIN, SELLER, BIDDER

        if (role != null) {
            String fxmlFile = "";
            // Xử lý chuyển màn hình riêng
            if (role.equalsIgnoreCase("ADMIN")) fxmlFile = "admin_dashboard.fxml";
            else if (role.equalsIgnoreCase("SELLER")) fxmlFile = "seller_dashboard.fxml";
            else fxmlFile = "main_dashboard.fxml";

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/" + fxmlFile));
                Parent root = loader.load();

                // Fix lỗi đỏ bằng cách gọi hàm setUserData đã thêm ở trên
                DashboardController controller = loader.getController();
                controller.setUserData(user);

                Stage stage = (Stage) txtUsername.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Đăng nhập thất bại!");
        }
    }
}
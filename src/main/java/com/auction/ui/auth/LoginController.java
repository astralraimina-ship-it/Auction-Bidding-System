package com.auction.ui.auth;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
import com.auction.network.ClientManager;
import com.auction.util.NavigationService; // Dùng helper mới tạo
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController implements ClientManager.UpdateListener {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize(){
        ClientManager.getInstance().addUpdateListener(this);
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        ClientManager.getInstance().sendCommand("LOGIN;" + username + ";" + password);
    }

    @Override
    public void onUpdateReceived(String signal) {
        if (signal.startsWith("LOGIN_SUCCESS")) {
            String[] parts = signal.split(";");
            int userId = Integer.parseInt(parts[1]);
            String username = parts[2];
            String role = parts[3];
            ClientManager.getInstance().removeUpdateListener(this);

            String fxml = getDashboardPath(role);
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            NavigationService.openDashboard(stage, fxml, userId, username, role);

        } else if (signal.startsWith("LOGIN_FAILED")) {
            String[] parts = signal.split(";");
            String errorMsg = parts[1];
            showAlert(errorMsg);
        }
    }

    // Tách logic chọn đường dẫn ra một hàm riêng cho gọn
    private String getDashboardPath(String role) {
        switch (role.toUpperCase()) {
            case "ADMIN":  return "/com/auction/ui/admin_dashboard.fxml";
            case "SELLER": return "/com/auction/ui/seller_dashboard.fxml";
            default:       return "/com/auction/ui/bidder_dashboard.fxml";
        }
    }

    @FXML
    private void goToRegister() {
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/Register.fxml", "UET Auction - Đăng ký");
    }

    private void showAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
package com.auction.ui.dashboard;

import com.auction.database.UserDAO;
import com.auction.ui.tab.AdminController;
import com.auction.ui.tab.BidderController;
import com.auction.ui.tab.SellerController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {
    @FXML private Label lblBalance;

    // Inject các controller con từ fx:include (fx:id + "Controller")
    @FXML private AdminController adminTabController;
    @FXML private SellerController sellerTabController;
    @FXML private BidderController bidderTabController;

    private UserDAO userDAO = new UserDAO();
    private String currentUsername;
    private int userId;

    public void setUserData(int userId, String username) {
        this.userId = userId;
        this.currentUsername = username;
        refreshBalance();

        // Truyền dữ liệu xuống các tab con sau khi login
        if (adminTabController != null) adminTabController.refreshData();
        if (sellerTabController != null) sellerTabController.setSellerInfo(userId, username);
        if (bidderTabController != null) bidderTabController.setBidderInfo(userId, username);
    }

    public void refreshBalance() {
        if (lblBalance != null && currentUsername != null) {
            double balance = userDAO.getUserBalance(currentUsername);
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/ui/login.fxml"));
            Stage stage = (Stage) lblBalance.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
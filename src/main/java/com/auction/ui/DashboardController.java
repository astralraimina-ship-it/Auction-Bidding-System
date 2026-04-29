package com.auction.ui;

import com.auction.database.UserDAO;
import com.auction.database.ItemDAO;
import com.auction.common.item.Item;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {
    @FXML private Label lblBalance;
    @FXML private TableView<Item> tableItems;

    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();

    public void setUserData(String username) {
        double balance = userDAO.getUserBalance(username);
        if (lblBalance != null) {
            lblBalance.setText(String.format("%,.2f VNĐ", balance));
        }
        loadData();
    }

    private void loadData() {
        if (tableItems != null) {
            tableItems.setItems(itemDAO.getAllItems());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // 1. Tải file FXML của màn hình Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
            Parent root = loader.load();

            // 2. Lấy Stage hiện tại từ bất kỳ component nào (ở đây dùng lblBalance hoặc tableItems)
            Stage stage = (Stage) tableItems.getScene().getWindow();

            // 3. Đặt Scene mới và hiển thị
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Login");
            stage.show();

            System.out.println("Đã đăng xuất thành công.");
        } catch (IOException e) {
            System.err.println("Lỗi khi chuyển về màn hình đăng nhập: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
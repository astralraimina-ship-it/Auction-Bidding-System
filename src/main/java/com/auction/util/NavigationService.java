package com.auction.util;

import com.auction.ui.tab.AdminController;
import com.auction.ui.tab.BidderController;
import com.auction.ui.tab.SellerController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationService {

    public static void navigate(Stage stage, String fxmlPath, String title) {
        try {
            // Sử dụng dấu / ở đầu fxmlPath khi gọi hàm này
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Hàm mở Dashboard đa năng: Điều hướng và truyền dữ liệu cho từng Controller riêng biệt
     */
    public static void openDashboard(Stage stage, String fxmlPath, int userId, String username, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Phân loại Controller dựa trên Role để truyền dữ liệu đúng cách
            switch (role.toUpperCase()) {
                case "ADMIN":
                    AdminController adminCtrl = loader.getController();
                    adminCtrl.refreshData();
                    break;

                case "BIDDER":
                    BidderController bidderCtrl = loader.getController();
                    bidderCtrl.setBidderInfo(username);
                    break;

                case "SELLER":
                    SellerController sellerCtrl = loader.getController();
                    sellerCtrl.setSellerInfo(userId, username);
                    break;

                default:
                    System.out.println("Cảnh báo: Role không xác định: " + role);
                    break;
            }

            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - " + role);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("Lỗi nạp giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassCastException e) {
            System.err.println("Lỗi ép kiểu: FXML không khớp với Controller tương ứng!");
            e.printStackTrace();
        }
    }
}
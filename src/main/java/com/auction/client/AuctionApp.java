package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class AuctionApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1. Lấy đường dẫn file FXML (Đảm bảo file nằm trong resources/com/auction/client/)
            URL fxmlLocation = getClass().getResource("/com/auction/client/login-view.fxml");

            // Kiểm tra an toàn: Nếu null là do đặt sai thư mục hoặc sai tên file
            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file login-view.fxml!");
                System.err.println("Hãy kiểm tra lại thư mục: src/main/resources/com/auction/client/");
                return;
            }

            // 2. Nạp giao diện
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            // 3. Hiển thị cửa sổ
            Scene scene = new Scene(root);
            stage.setTitle("UET Auction System - Login");
            stage.setScene(scene);
            stage.setResizable(false); // Khóa kích thước cho đẹp
            stage.show();

            System.out.println("Khởi động màn hình Login thành công!");

        } catch (Exception e) {
            System.err.println("LỖI KHỞI CHẠY:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class AuctionApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // 1. Lấy đường dẫn file FXML (Đảm bảo file nằm trong resources/com/auction/client/)
        URL fxmlLocation = getClass().getResource("/com/auction/client/login-view.fxml");

        // 2. Kiểm tra an toàn: Nếu null là do đặt sai thư mục trong resources
        if (fxmlLocation == null) {
            System.err.println("Không tìm thấy file login-view.fxml");
            System.err.println("Hãy kiểm tra lại folder: src/main/resources/com/auction/client/");
            return;
        }

        // 3. Nạp giao diện
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(loader.load());

        stage.setTitle("UET Auction System - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
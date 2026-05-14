package com.auction.client;

import com.auction.network.ClientManager;
import com.auction.server.AuctionServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);
        stage.setTitle("Login - Auction System");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Phương thức stop() được JavaFX tự động gọi khi người dùng đóng ứng dụng.
     * Đây là nơi lý tưởng để dọn dẹp kết nối ngầm.
     */
    @Override
    public void stop() throws Exception {
        ClientManager.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        ClientManager.getInstance().startListening();
        launch();
    }
}
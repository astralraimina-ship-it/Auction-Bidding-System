package com.auction.ui;

import com.auction.common.auction.Auction;
import com.auction.common.auction.AuctionObserver;
import com.auction.common.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionDetailController implements AuctionObserver {
    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtLogs;

    private Auction auction;
    private User currentUser;

    // Hàm này sẽ được gọi khi chuyển từ Dashboard sang Detail
    public void setAuctionData(Auction auction) {
        this.auction = auction;

        // Đăng ký GUI này nhận thông báo từ Auction
        this.auction.addObserver(this);

        // Hiển thị dữ liệu ban đầu
        lblItemName.setText(auction.getItem().getName());
        lblCurrentPrice.setText(String.valueOf(auction.getCurrentPrice()));
    }

    @FXML
    public void handleBid() {
        try {
            double amount = Double.parseDouble(txtBidAmount.getText());
            auction.update(currentUser, amount); // Gọi hàm update thread-safe đã viết
        } catch (NumberFormatException e) {
            txtLogs.appendText("[LOI]: Vui long nhap so tien hop le!\n");
        }
    }

    @FXML
    public void handleBuyNow() {
        auction.buyItNow(currentUser);
    }

    @Override
    public void onUpdate(String message) {
        // CẬP NHẬT GIAO DIỆN TỪ LUỒNG KHÁC (Mấu chốt của đồ án)
        Platform.runLater(() -> {
            txtLogs.appendText(message + "\n");
            lblCurrentPrice.setText(String.valueOf(auction.getCurrentPrice()));
        });
    }
}
package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Optional;

public class BidderAuctionRoomController implements ClientManager.UpdateListener {
    @FXML private Label lblProductName, lblCurrentPrice, lblStep, lblBinPrice;
    @FXML private TextField txtBidInput;
    @FXML private TextArea txtAreaLog;
    @FXML private Button btnPlaceBid;

    private Item currentItem;
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final Object lock = new Object();

    private int currentUserId;

    /**
     * Nhận dữ liệu Item và hiển thị lên UI
     */
    public void initData(Item item) {
        this.currentItem = item;
        lblProductName.setText(item.getName());
        lblStep.setText("Bước giá tối thiểu: " + String.format("%,.0f", item.getStep()) + " VNĐ");
        lblBinPrice.setText("Giá mua đứt: " + String.format("%,.0f", item.getBinPrice()) + " VNĐ");
        manualRefresh();

        // ĐĂNG KÝ LISTENER AN TOÀN (Dùng 'this' vì class đã implement UpdateListener)
        ClientManager.getInstance().addUpdateListener(this);
    }

    /**
     * Triển khai hàm onUpdateReceived từ interface UpdateListener
     * Tất cả các tín hiệu real-time tự động đổ về đây và xử lý an toàn trong Platform.runLater
     */
    @Override
    public void onUpdateReceived(String signal) {
        Platform.runLater(() -> {
            // Sử dụng equalsIgnoreCase để loại bỏ hoàn toàn lỗi lệch chữ HOA / chữ thường
            if (signal.equalsIgnoreCase("REFRESH")) {
                manualRefresh();
            }
            else if (signal.equalsIgnoreCase("AntiSnipe")) {
                handleAntiSnipe(currentItem);
            }
            else if (signal.equalsIgnoreCase("Closed")) {
                logAction("Hệ thống: Phiên đấu giá đã kết thúc.");
                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                manualRefresh();
            }
            else if (signal.startsWith("Notify;")) {
                logAction(signal.substring(7));
            }
            else if (signal.startsWith("Error;")) {
                showError(signal.substring(6));
            }
        });
    }

    /**
     * Nhận ID và Username của người dùng từ Dashboard
     */
    public void setUserId(int id) {
        this.currentUserId = id;
    }

    @FXML
    private void handlePlaceBid() {
        String input = txtBidInput.getText();
        try {
            double amount = Double.parseDouble(input);
            processBidLogic(amount);
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    private void processBidLogic(double bidAmount) {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            long endTime = currentItem.getEndTime().getTime();

            if (currentTime >= endTime) {
                showError("Rất tiếc! Phiên đấu giá này đã kết thúc.");
                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                return;
            }

            double currentMax = bidDAO.getCurrentMaxBid(currentItem.getId(), currentItem.getStartPrice());

            if (bidAmount >= currentItem.getBinPrice()) {
                handleBinConfirmation();
                return;
            }

            if (bidAmount < (currentMax + currentItem.getStep())) {
                showError("Giá đặt phải cao hơn " + String.format("%,.0f", (currentMax + currentItem.getStep())) + " VNĐ");
                return;
            }

            ClientManager.getInstance().sendCommand("BID;" + currentItem.getId() + ";" + currentUserId + ";" + bidAmount);
        }
    }

    private void handleBinConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận mua đứt");
        alert.setHeaderText("Giá bạn đưa ra đạt ngưỡng Mua Đứt!");
        alert.setContentText("Bạn có muốn mua luôn sản phẩm này với giá " + String.format("%,.0f", currentItem.getBinPrice()) + " VNĐ không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ClientManager.getInstance().sendCommand("BIN;" + currentItem.getId() + ";" + currentUserId + ";" + currentItem.getBinPrice());
        }
    }

    private void handleAntiSnipe(Item item) {
        if (item.getEndTime() == null) return;

        long timeLeft = item.getEndTime().getTime() - System.currentTimeMillis();

        if (timeLeft > 0 && timeLeft < 60000) {
            int minutesToExtend = 2;
            boolean success = itemDAO.extendAuctionTime(item.getId(), minutesToExtend);

            if (success) {
                long newEndTimeMillis = item.getEndTime().getTime() + (minutesToExtend * 60 * 1000);
                item.setEndTime(new java.sql.Timestamp(newEndTimeMillis));

                logAction("Hệ thống: Phát hiện đấu giá sát nút! Tự động gia hạn thêm " + minutesToExtend + " phút.");
            }
        }
    }

    @FXML
    public void manualRefresh() {
        double max = bidDAO.getCurrentMaxBid(currentItem.getId(), currentItem.getStartPrice());
        Platform.runLater(() -> {
            lblCurrentPrice.setText("GIÁ HIỆN TẠI: " + String.format("%,.0f", max) + " VNĐ");
        });
    }

    private void logAction(String msg) {
        txtAreaLog.appendText("> " + msg + "\n");
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.show();
    }
}
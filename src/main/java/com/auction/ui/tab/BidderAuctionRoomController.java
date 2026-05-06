package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

public class BidderAuctionRoomController {
    @FXML private Label lblProductName, lblCurrentPrice, lblStep, lblBinPrice;
    @FXML private TextField txtBidInput;
    @FXML private TextArea txtAreaLog;
    @FXML private Button btnPlaceBid;

    private Item currentItem;
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final Object lock = new Object();

    // ID người dùng thực tế sẽ được truyền vào qua hàm setUserId
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
    }

    /**
     * Nhận ID người dùng từ Dashboard
     */
    public void setUserId(int id) {
        this.currentUserId = id;
        logAction("Hệ thống: Đã xác thực người dùng ID #" + id);
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
            // --- CHỐT CHẶN THỜI GIAN (100% CẤM BID KHI HẾT GIỜ) ---
            long currentTime = System.currentTimeMillis();
            long endTime = currentItem.getEndTime().getTime();

            if (currentTime >= endTime) {
                showError("Rất tiếc! Phiên đấu giá này đã kết thúc.");
                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                return;
            }

            double currentMax = bidDAO.getCurrentMaxBid(currentItem.getId(), currentItem.getStartPrice());

            // Kiểm tra ngưỡng mua đứt
            if (bidAmount >= currentItem.getBinPrice()) {
                handleBinConfirmation();
                return;
            }

            // Kiểm tra bước giá
            if (bidAmount < (currentMax + currentItem.getStep())) {
                showError("Giá đặt phải cao hơn " + String.format("%,.0f", (currentMax + currentItem.getStep())) + " VNĐ");
                return;
            }

            // Ghi nhận lượt bid vào Database
            boolean success = bidDAO.placeBid(currentItem.getId(), currentUserId, bidAmount);
            if (success) {
                logAction("BẠN đã đặt giá thành công: " + String.format("%,.0f", bidAmount) + " VNĐ");

                // Kích hoạt Anti-Snipe nếu bid ở những giây cuối
                handleAntiSnipe(currentItem);

                manualRefresh();
            } else {
                showError("Đặt giá thất bại! Có thể phiên đấu giá đã đóng trên máy chủ.");
            }
        }
    }

    private void handleBinConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận mua đứt");
        alert.setHeaderText("Giá bạn đưa ra đạt ngưỡng Mua Đứt!");
        alert.setContentText("Bạn có muốn mua luôn sản phẩm này với giá " + String.format("%,.0f", currentItem.getBinPrice()) + " VNĐ không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean bidSuccess = bidDAO.placeBid(currentItem.getId(), currentUserId, currentItem.getBinPrice());

            if (bidSuccess) {
                boolean updateSuccess = itemDAO.closeAuction(currentItem.getId(), currentUserId);
                if (updateSuccess) {
                    logAction("CHÚC MỪNG! Bạn đã mua đứt thành công.");
                    logAction("Hệ thống: Phiên đấu giá đã kết thúc.");

                    txtBidInput.setDisable(true);
                    btnPlaceBid.setDisable(true);
                    manualRefresh();
                }
            }
        }
    }

    private void handleAntiSnipe(Item item) {
        if (item.getEndTime() == null) return;

        long timeLeft = item.getEndTime().getTime() - System.currentTimeMillis();

        // Nếu còn dưới 1 phút, tự động gia hạn thêm 2 phút
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
package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;
import java.util.Optional;

public class BidderAuctionRoomController implements ClientManager.UpdateListener {
    @FXML private Label lblProductName, lblCurrentPrice, lblStep, lblBinPrice;
    @FXML private TextField txtBidInput;

    // ĐÃ BỔ SUNG: Khai báo 2 ô nhập liệu trực tiếp từ giao diện FXML mới
    @FXML private TextField txtAutoStep;
    @FXML private TextField txtStopPrice;

    @FXML private TextArea txtAreaLog;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnAutoBidSetup; // Nút màu cam "KÍCH HOẠT AUTO-BID" của bạn

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

        // ĐÃ BỔ SUNG: Gợi ý sẵn (PromptText) số tiền hợp lệ vào 2 ô nhập để người dùng dễ nhìn
        if (txtAutoStep != null) txtAutoStep.setPromptText("Ví dụ: " + String.format("%.0f", item.getStep()));
        if (txtStopPrice != null) txtStopPrice.setPromptText("Ví dụ: " + String.format("%.0f", item.getBinPrice()));

        manualRefresh();
        loadHistoryFromDatabase();

        // ĐĂNG KÝ LISTENER AN TOÀN (Dùng 'this' vì class đã implement UpdateListener)
        ClientManager.getInstance().addUpdateListener(this);
    }

    /**
     * Tải lịch sử đấu giá từ Database lên ô văn bản nhật ký
     */
    private void loadHistoryFromDatabase() {
        if (currentItem == null) return;

        try {
            List<String> logs = bidDAO.getBidHistoryText(currentItem.getId());
            txtAreaLog.clear();
            for (String log : logs) {
                txtAreaLog.appendText(log + "\n");
            }
        } catch (Exception e) {
            showError("Không thể tải dữ liệu từ cơ sở dữ liệu Cloud. Vui lòng kiểm tra kết nối Internet!");
            e.printStackTrace();
        }
    }

    /**
     * ĐÃ SỬA ĐỔI TRIỆT ĐỂ: Lấy dữ liệu trực tiếp từ giao diện, XÓA BỎ HỘP THOẠI DIALOG RÁC.
     * Ủy quyền tác vụ đẩy giá lên Server để chạy độc lập khi Client đóng phòng.
     */
    @FXML
    private void handleAutoBidSetup() {
        if (currentItem == null) return;

        // 1. Đọc dữ liệu trực tiếp từ 2 ô nhập trên UI
        String stepStr = txtAutoStep.getText().trim();
        String stopStr = txtStopPrice.getText().trim();

        // Kiểm tra xem người dùng có bỏ trống ô nào không
        if (stepStr.isEmpty() || stopStr.isEmpty()) {
            showError("Vui lòng nhập đầy đủ cả Bước nhảy Auto và Ngưỡng dừng tối đa!");
            return;
        }

        try {
            double autoStep = Double.parseDouble(stepStr);
            double stopPrice = Double.parseDouble(stopStr);

            // RÀNG BUỘC 1: Bước nhảy tự động phải LỚN HƠN HOẶC BẰNG bước nhảy của sản phẩm
            if (autoStep < currentItem.getStep()) {
                showError("Lỗi cấu hình: Bước nhảy tự động phải LỚN HƠN HOẶC BẰNG bước nhảy gốc của sản phẩm (" + String.format("%,.0f", currentItem.getStep()) + " VNĐ)!");
                return;
            }

            // RÀNG BUỘC 2: Ngưỡng dừng không được lớn hơn bin price (giá mua đứt)
            if (stopPrice > currentItem.getBinPrice()) {
                showError("Lỗi cấu hình: Ngưỡng dừng tối đa không được vượt quá Giá mua đứt (" + String.format("%,.0f", currentItem.getBinPrice()) + " VNĐ)!");
                return;
            }

            // 2. Gửi lệnh cấu hình thẳng lên Server qua Socket trung tâm để Server chịu trách nhiệm lưu trữ chạy ngầm
            // Cấu trúc chuỗi gửi: SET_AUTOBID;itemId;userId;autoStep;stopPrice
            String cmd = "SET_AUTOBID;" + currentItem.getId() + ";" + currentUserId + ";" + autoStep + ";" + stopPrice;
            ClientManager.getInstance().sendCommand(cmd);

            // Ghi nhận trực tiếp trạng thái lên Nhật ký phòng đấu giá nội bộ
            logAction("Hệ thống: Đã kích hoạt Auto-Bid thành công (Bước nhảy: "
                    + String.format("%,.0f", autoStep) + " VNĐ | Ngưỡng dừng: "
                    + String.format("%,.0f", stopPrice) + " VNĐ). Nhiệm vụ đã giao cho Server xử lý ngầm.");

            // 3. Xóa trắng các ô nhập liệu cho sạch giao diện sau khi cài đặt thành công
            txtAutoStep.clear();
            txtStopPrice.clear();

        } catch (NumberFormatException e) {
            showError("Định dạng nhập vào không hợp lệ! Vui lòng chỉ nhập các ký tự số.");
        }
    }

    /**
     * Triển khai hàm onUpdateReceived từ interface UpdateListener
     * Tất cả các tín hiệu real-time tự động đổ về đây và xử lý an toàn trong Platform.runLater
     */
    @Override
    public void onUpdateReceived(String signal) {
        Platform.runLater(() -> {
            if (signal.equalsIgnoreCase("REFRESH")) {
                manualRefresh();
            }
            else if (signal.equalsIgnoreCase("AntiSnipe")) {
                handleAntiSnipe(currentItem);
            }
            else if (signal.equalsIgnoreCase("Closed")) {
                logAction("Hệ thống: Phiên đấu giá đã kết thúc.");

                // Khóa toàn bộ các ô nhập và nút bấm liên quan khi phòng đã đóng cửa
                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                if (btnAutoBidSetup != null) btnAutoBidSetup.setDisable(true);
                if (txtAutoStep != null) txtAutoStep.setDisable(true);
                if (txtStopPrice != null) txtStopPrice.setDisable(true);

                manualRefresh();
            }
            else if (signal.startsWith("Notify;")) {
                logAction(signal.substring(7));
            }
            else if (signal.startsWith("BID_UPDATE")){
                String[] parts = signal.split(";");
                int updatedItemId = Integer.parseInt(parts[1]);
                int bidderId = Integer.parseInt(parts[2]);
                double amount = Double.parseDouble(parts[3]);

                if (currentItem != null && currentItem.getId() == updatedItemId) {
                    String newLog = "User ID " + bidderId + " đã đặt giá " + String.format("%,.0f", amount) + " VNĐ";
                    txtAreaLog.appendText(newLog + "\n");
                    loadHistoryFromDatabase();
                    manualRefresh();
                }
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
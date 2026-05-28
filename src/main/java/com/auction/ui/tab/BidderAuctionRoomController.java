package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class BidderAuctionRoomController implements ClientManager.UpdateListener {
    @FXML private Label lblProductName, lblCurrentPrice, lblStep, lblBinPrice;
    @FXML private TextField txtBidInput;
    @FXML private TextField txtStopPrice;

    @FXML private TextArea txtAreaLog;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnAutoBidSetup;
    @FXML private Button btnStopAutoBid;

    // Khung hiển thị ảnh sản phẩm
    @FXML private ImageView itemImageView;

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

        if (txtStopPrice != null) txtStopPrice.setPromptText("Ví dụ: " + String.format("%.0f", item.getBinPrice()));

        // 🔥 ĐÃ SỬA: Xử lý hiển thị ảnh mặc định từ resources nếu trống ảnh
        if (itemImageView != null) {
            String imagePath = item.getImagePath();
            if (imagePath == null || imagePath.isEmpty() || imagePath.equals("default.png")) {
                loadDefaultImage(); // Gọi hàm load ảnh mặc định
            } else {
                // Tận dụng Cache tĩnh từ SellerController để tránh tải lại ảnh
                if (SellerController.imageCache.containsKey(imagePath)) {
                    itemImageView.setImage(SellerController.imageCache.get(imagePath));
                } else {
                    // Nếu chưa có ảnh trong RAM, gửi lệnh tải từ Server
                    ClientManager.getInstance().sendCommand("GET_IMAGE;" + imagePath);
                }
            }
        }

        manualRefresh();
        loadHistoryFromDatabase();

        ClientManager.getInstance().sendCommand("CHECK_AUTOBID_STATUS;" + item.getId() + ";" + currentUserId);
        ClientManager.getInstance().addUpdateListener(this);
    }

    /**
     * 🔥 THÊM MỚI: Hàm nạp ảnh mặc định từ resources (An toàn, chống Crash)
     */
    private void loadDefaultImage() {
        if (itemImageView == null) return;
        try {
            // Thử tìm tại thư mục src/main/resources/images/default.png
            Image defaultImg = new Image(getClass().getResourceAsStream("/images/default.png"));
            itemImageView.setImage(defaultImg);
        } catch (Exception e) {
            try {
                // Fallback: Thử tìm tại thư mục src/main/resources/com/auction/ui/images/default.png theo package cấu trúc của bạn
                Image defaultImg = new Image(getClass().getResourceAsStream("/images/default.png"));
                itemImageView.setImage(defaultImg);
            } catch (Exception ex) {
                System.out.println("Lỗi: Không tìm thấy file default.png ở cả 2 đường dẫn tài nguyên. Khung ảnh sẽ để trống.");
                itemImageView.setImage(null);
            }
        }
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
            showError("Không thể tải dữ liệu từ cơ sở dữ liệu. Vui lòng kiểm tra kết nối Internet!");
            e.printStackTrace();
        }
    }

    /**
     * XỬ LÝ KÍCH HOẠT AUTO-BID & KHÓA ĐẤU GIÁ THỦ CÔNG
     */
    @FXML
    private void handleAutoBidSetup() {
        if (currentItem == null) return;

        String stopStr = txtStopPrice.getText().trim();

        if (stopStr.isEmpty()) {
            showError("Vui lòng nhập Ngưỡng dừng tối đa muốn cài đặt!");
            return;
        }

        try {
            double stopPrice = Double.parseDouble(stopStr);
            double currentMax = bidDAO.getCurrentMaxBid(currentItem.getId(), currentItem.getStartPrice());

            if (stopPrice > currentItem.getBinPrice()) {
                showError("Lỗi cấu hình: Ngưỡng dừng tối đa không được vượt quá Giá mua đứt (" + String.format("%,.0f", currentItem.getBinPrice()) + " VNĐ)!");
                return;
            }

            if (stopPrice < currentMax) {
                showError("Lỗi cấu hình: Ngưỡng dừng tối đa không được thấp hơn Giá hiện tại (" + String.format("%,.0f", currentMax) + " VNĐ)!");
                return;
            }

            double autoStep = currentItem.getStep();

            String cmd = "SET_AUTOBID;" + currentItem.getId() + ";" + currentUserId + ";" + autoStep + ";" + stopPrice;
            ClientManager.getInstance().sendCommand(cmd);

            txtBidInput.setDisable(true);
            btnPlaceBid.setDisable(true);
            txtStopPrice.setDisable(true);
            btnAutoBidSetup.setDisable(true);

            btnStopAutoBid.setDisable(false);

            logAction("Hệ thống: Đã bật chế độ Đấu giá tự động thành công (Theo bước giá gốc: "
                    + String.format("%,.0f", autoStep) + " VNĐ | Ngưỡng dừng: "
                    + String.format("%,.0f", stopPrice) + " VNĐ). ĐÃ KHÓA ĐẶT GIÁ THỦ CÔNG!");

            txtStopPrice.clear();

        } catch (NumberFormatException e) {
            showError("Định dạng nhập vào không hợp lệ! Vui lòng chỉ nhập các ký tự số.");
        }
    }

    /**
     * Xử lý nút HỦY/DỪNG chế độ đấu giá tự động
     */
    @FXML
    private void handleStopAutoBid() {
        if (currentItem == null) return;

        String cmd = "STOP_AUTOBID;" + currentItem.getId() + ";" + currentUserId;
        ClientManager.getInstance().sendCommand(cmd);

        txtBidInput.setDisable(false);
        btnPlaceBid.setDisable(false);
        txtStopPrice.setDisable(false);
        btnAutoBidSetup.setDisable(false);

        btnStopAutoBid.setDisable(true);

        logAction("Hệ thống: Đã tắt chế độ Auto-Bid. Bạn có thể tự đặt giá thủ công trở lại.");
    }

    /**
     * Tất cả các tín hiệu real-time tự động đổ về đây
     */
    @Override
    public void onUpdateReceived(String signal) {
        Platform.runLater(() -> {
            if (signal.equalsIgnoreCase("REFRESH")) {
                manualRefresh();
            }
            // Xử lý khi Server gửi ảnh về
            else if (signal.startsWith("IMAGE_RESPONSE;")) {
                String[] parts = signal.split(";", 3);
                if (parts.length == 3) {
                    String fileName = parts[1];
                    String base64 = parts[2];

                    if (currentItem != null && fileName.equals(currentItem.getImagePath())) {
                        try {
                            byte[] decoded = Base64.getDecoder().decode(base64);
                            ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
                            Image img = new Image(bis);

                            SellerController.imageCache.put(fileName, img);
                            if (itemImageView != null) {
                                itemImageView.setImage(img);
                            }
                        } catch (Exception e) {
                            System.out.println("Lỗi giải mã ảnh từ server: " + e.getMessage());
                            // 🔥 ĐÃ SỬA: Nếu giải mã ảnh lỗi (file hỏng, mất mạng giữa chừng), chuyển sang ảnh mặc định
                            loadDefaultImage();
                        }
                    }
                }
            }
            else if (signal.equalsIgnoreCase("AntiSnipe")) {
                handleAntiSnipe(currentItem);
            }
            else if (signal.equalsIgnoreCase("Closed")) {
                logAction("Hệ thống: Phiên đấu giá đã kết thúc.");

                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                if (btnAutoBidSetup != null) btnAutoBidSetup.setDisable(true);
                if (btnStopAutoBid != null) btnStopAutoBid.setDisable(true);
                if (txtStopPrice != null) txtStopPrice.setDisable(true);

                manualRefresh();
            }
            else if (signal.startsWith("AUTOBID_STATUS")) {
                String[] part = signal.split(";");
                String status = part[1];
                int userId = Integer.parseInt(part[2]);

                if (currentUserId == userId){
                    if (status.equals("ACTIVE")){
                        txtBidInput.setDisable(true);
                        btnPlaceBid.setDisable(true);
                        txtStopPrice.setDisable(true);
                        btnAutoBidSetup.setDisable(true);
                        btnStopAutoBid.setDisable(false);

                        if (part.length > 3) {
                            double budget = Double.parseDouble(part[3]);
                            logAction("Hệ thống: Bạn đang trong trạng thái TỰ ĐỘNG ĐẤU GIÁ với ngân sách tối đa "
                                    + String.format("%,.0f", budget) + " VNĐ.");
                        }
                    }
                    else if (status.equals("INACTIVE")) {
                        txtBidInput.setDisable(false);
                        btnPlaceBid.setDisable(false);
                        txtStopPrice.setDisable(false);
                        btnAutoBidSetup.setDisable(false);
                        btnStopAutoBid.setDisable(true);

                        logAction("Hệ thống: Chế độ Autobid của bạn hiện đang tắt / Đã bị hủy.");
                    }
                }
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
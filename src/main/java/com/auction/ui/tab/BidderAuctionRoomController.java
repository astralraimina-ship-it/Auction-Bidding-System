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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    // Khai báo các thành phần điều khiển LineChart
    @FXML private LineChart<String, Number> priceLineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private XYChart.Series<String, Number> priceSeries;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Item currentItem;
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final Object lock = new Object();

    private int currentUserId;

    // Phương thức khởi tạo cấu trúc dữ liệu cho biểu đồ
    @FXML
    public void initialize() {
        if (priceLineChart != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Mức giá hiện tại");
            priceLineChart.getData().add(priceSeries);
            priceLineChart.setAnimated(false);
            // Tắt hiệu ứng chuyển động mặc định để chống lag UI
            priceLineChart.setCreateSymbols(false);
        }
    }

    /**
     * Nhận dữ liệu Item và hiển thị lên UI
     */
    public void initData(Item item) {
        if (item == null) return;
        this.currentItem = item;

        // 🔥 FIX 1: Xóa sạch dữ liệu điểm vẽ cũ của sản phẩm trước khi đổi phòng đấu giá mới
        if (priceSeries != null) {
            priceSeries.getData().clear();
        }

        lblProductName.setText(item.getName());
        lblStep.setText("Bước giá tối thiểu: " + String.format("%,.0f", item.getStep()) + " VNĐ");
        lblBinPrice.setText("Giá mua đứt: " + String.format("%,.0f", item.getBinPrice()) + " VNĐ");

        if (txtStopPrice != null) {
            txtStopPrice.setPromptText("Ví dụ: " + String.format("%.0f", item.getBinPrice()));
        }

        // XỬ LÝ TỰ ĐỘNG NHẬN DIỆN ẢNH CLOUDINARY HOẶC LOCAL
        if (itemImageView != null) {
            String imagePath = item.getImagePath();
            if (imagePath == null || imagePath.isEmpty() || imagePath.equals("default.png")) {
                loadDefaultImage();
            } else {
                // TRƯỜNG HỢP 1: Nếu là link online Cloudinary (Bắt đầu bằng http hoặc https)
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    try {
                        // JavaFX hỗ trợ truyền URL trực tiếp vào đối tượng Image, đặt 'true' để load background tránh đơ UI
                        Image img = new Image(imagePath, true);
                        itemImageView.setImage(img);
                    } catch (Exception e) {
                        System.out.println("Lỗi tải ảnh trực tiếp từ URL Cloudinary: " + e.getMessage());
                        loadDefaultImage();
                    }
                }
                // TRƯỜNG HỢP 2: Tận dụng Cache tĩnh từ SellerController
                else if (SellerController.imageCache != null && SellerController.imageCache.containsKey(imagePath)) {
                    itemImageView.setImage(SellerController.imageCache.get(imagePath));
                }
                // TRƯỜNG HỢP 3: Nếu là ảnh cục bộ chưa có trong RAM, gửi lệnh tải từ Server qua Socket
                else {
                    ClientManager.getInstance().sendCommand("GET_IMAGE;" + imagePath);
                }
            }
        }

        // Tải lịch sử văn bản nhật ký trước
        loadHistoryFromDatabase();

        // Hiển thị text giá hiện tại lên Label trước
        if (lblCurrentPrice != null) {
            double max = bidDAO.getCurrentMaxBid(item.getId(), item.getStartPrice());
            lblCurrentPrice.setText("GIÁ HIỆN TẠI: " + String.format("%,.0f", max) + " VNĐ");
        }

        // 🔥 SỬA CHÍNH: Tạo luồng riêng tải toàn bộ lịch sử điểm vẽ đồ thị từ Database lên
        new Thread(() -> {
            List<BidDAO.BidHistoryPoint> historyPoints = bidDAO.getBidHistoryOfItem(item.getId());

            Platform.runLater(() -> {
                if (priceSeries != null) {
                    if (historyPoints.isEmpty()) {
                        // Nếu chưa có ai đặt giá bao giờ, vẽ 1 điểm làm mốc ban đầu (Giá khởi điểm)
                        String currentTime = LocalTime.now().format(timeFormatter);
                        priceSeries.getData().add(new XYChart.Data<>(currentTime, item.getStartPrice()));
                    } else {
                        // Nếu ĐÃ CÓ lịch sử, dùng vòng lặp vẽ lại toàn bộ các điểm cũ theo đúng thứ tự thời gian
                        for (BidDAO.BidHistoryPoint point : historyPoints) {
                            priceSeries.getData().add(new XYChart.Data<>(point.timeLabel, point.price));
                        }
                    }

                    // Giới hạn hiển thị 15 điểm mốc gần nhất để tránh quá dày sinh lag đồ thị
                    if (priceSeries.getData().size() > 15) {
                        int totalSize = priceSeries.getData().size();
                        priceSeries.getData().remove(0, totalSize - 15);
                    }
                }
            });
        }).start();

        ClientManager.getInstance().sendCommand("CHECK_AUTOBID_STATUS;" + item.getId() + ";" + currentUserId);
        ClientManager.getInstance().addUpdateListener(this);
    }

    /**
     * Hàm hỗ trợ cập nhật dữ liệu lên LineChart một cách Thread-safe
     */
    private void updateChartRealtime(double newPrice) {
        Platform.runLater(() -> {
            if (priceSeries != null) {
                String currentTime = LocalTime.now().format(timeFormatter);
                priceSeries.getData().add(new XYChart.Data<>(currentTime, newPrice));

                // Giới hạn hiển thị 15 điểm mốc gần nhất để biểu đồ tự cuốn ngang, tránh quá dày sinh lag
                if (priceSeries.getData().size() > 15) {
                    priceSeries.getData().remove(0);
                }
            }
        });
    }

    /**
     * Hàm nạp ảnh mặc định từ resources (An toàn, chống Crash)
     */
    private void loadDefaultImage() {
        if (itemImageView == null) return;
        try {
            // Thử tìm tại thư mục gốc src/main/resources/images/default.png
            Image defaultImg = new Image(getClass().getResourceAsStream("/images/default.png"));
            itemImageView.setImage(defaultImg);
        } catch (Exception e) {
            try {
                // Fallback: Thử tìm tại thư mục theo cấu trúc package com/auction/ui/images/default.png
                Image defaultImg = new Image(getClass().getResourceAsStream("/com/auction/ui/images/default.png"));
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
            // Xử lý khi Server phản hồi dữ liệu ảnh
            else if (signal.startsWith("IMAGE_RESPONSE;")) {
                String[] parts = signal.split(";", 3);
                if (parts.length == 3) {
                    String fileName = parts[1];
                    String content = parts[2];

                    if (currentItem != null && fileName.equals(currentItem.getImagePath())) {
                        try {
                            Image img;
                            // Nếu nội dung phản hồi là URL Cloudinary
                            if (content.startsWith("http://") || content.startsWith("https://")) {
                                img = new Image(content, true);
                            } else {
                                // Nếu nội dung phản hồi là chuỗi dữ liệu Base64 truyền thống
                                byte[] decoded = Base64.getDecoder().decode(content);
                                ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
                                img = new Image(bis);
                            }

                            if (SellerController.imageCache != null) {
                                SellerController.imageCache.put(fileName, img);
                            }
                            if (itemImageView != null) {
                                itemImageView.setImage(img);
                            }
                        } catch (Exception e) {
                            System.out.println("Lỗi xử lý ảnh từ dữ liệu server: " + e.getMessage());
                            loadDefaultImage();
                        }
                    }
                }
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
            // ================================================================
            // XỬ LÝ LỆNH BỊ HỦY AUTO-BID ĐỊNH DANH TỪ SERVER
            // ================================================================
            else if (signal.startsWith("AUTOBID_DISABLED;")) {
                String[] parts = signal.split(";");
                int disabledItemId = Integer.parseInt(parts[1]);
                int targetUserId = Integer.parseInt(parts[2]);

                // Đúng ID của mình và đúng sản phẩm tại phòng này mới thực thi ngắt giao diện
                if (currentUserId == targetUserId && currentItem != null && currentItem.getId() == disabledItemId) {
                    txtBidInput.setDisable(false);
                    btnPlaceBid.setDisable(false);
                    txtStopPrice.setDisable(false);
                    btnAutoBidSetup.setDisable(false);
                    btnStopAutoBid.setDisable(true);

                    logAction("Hệ thống: Chế độ Auto-Bid của bạn đã tự động ngắt (Do có người đặt giá áp đảo vượt hạn mức cấu hình)!");
                }
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
            } else if (signal.startsWith("ITEM_DELETED")){
                String[] part = signal.split(";");
                int itemId = Integer.parseInt(part[1]);
                if (itemId == currentItem.getId()){
                    txtBidInput.setDisable(true);
                    btnPlaceBid.setDisable(true);
                    if (btnAutoBidSetup != null) btnAutoBidSetup.setDisable(true);
                    if (btnStopAutoBid != null) btnStopAutoBid.setDisable(true);
                    if (txtStopPrice != null) txtStopPrice.setDisable(true);
                    logAction("Sản phẩm đã bị hủy");
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

                    // Đẩy mức giá mới nhận được từ phòng Socket lên đồ thị LineChart
                    updateChartRealtime(amount);
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
        if (currentItem == null) return;
        String input = txtBidInput.getText().trim();
        try {
            double amount = Double.parseDouble(input);
            processBidLogic(amount);
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    private void processBidLogic(double bidAmount) {
        synchronized (lock) {
            if (currentItem == null) {
                showError("Lỗi: Không tìm thấy thông tin sản phẩm hiện tại!");
                return;
            }

            // Kiểm tra nếu endTime bị null thì không gọi .getTime() để tránh sập app
            if (currentItem.getEndTime() == null) {
                showError("Rất tiếc! Phiên đấu giá này không tồn tại hoặc đã kết thúc trên hệ thống.");
                txtBidInput.setDisable(true);
                btnPlaceBid.setDisable(true);
                return;
            }

            long currentTime = System.currentTimeMillis();
            long endTime = currentItem.getEndTime().getTime(); // Hết lo bị NullPointerException ở đây nhé!

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
        if (currentItem == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận mua đứt");
        alert.setHeaderText("Giá bạn đưa ra đạt ngưỡng Mua Đứt!");
        alert.setContentText("Bạn có muốn mua luôn sản phẩm này với giá " + String.format("%,.0f", currentItem.getBinPrice()) + " VNĐ không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ClientManager.getInstance().sendCommand("BIN;" + currentItem.getId() + ";" + currentUserId + ";" + currentItem.getBinPrice());
        }
    }

    @FXML
    public void manualRefresh() {
        if (currentItem == null) return;
        double max = bidDAO.getCurrentMaxBid(currentItem.getId(), currentItem.getStartPrice());
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) {
                lblCurrentPrice.setText("GIÁ HIỆN TẠI: " + String.format("%,.0f", max) + " VNĐ");
            }
            // 🔥 FIX 2: Đồng bộ vẽ luôn điểm giá mới nhất này lên đồ thị khi bấm làm mới thủ công
            updateChartRealtime(max);
        });
    }

    private void logAction(String msg) {
        if (txtAreaLog != null) {
            txtAreaLog.appendText("> " + msg + "\n");
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.show();
    }
}
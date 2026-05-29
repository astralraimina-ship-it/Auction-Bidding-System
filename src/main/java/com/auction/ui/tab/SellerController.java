package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.database.ItemDAO;
import com.auction.database.SellerDAO;
import com.auction.network.ClientManager;
import com.auction.ui.dialogs.AddItemController;
import com.auction.ui.dialogs.TransactionController;
import com.auction.util.NavigationService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SellerController implements ClientManager.UpdateListener {
    @FXML private Label lblBalance;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colDetails;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;
    @FXML private TableColumn<Item, Timestamp> colTimeLeft;
    @FXML private TableColumn<Item, String> colPayStatus;

    // 🔥 CỘT ẢNH
    @FXML private TableColumn<Item, String> colImage;
    @FXML private Button btnRefresh;

    private SellerDAO sellerDAO = new SellerDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private int sellerId;
    private String username;

    // Giữ lại Cache tĩnh phòng trường hợp các file controller khác chưa sửa hết vẫn tham chiếu tới nó (không lo lỗi compile)
    public static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        setupColumns();

        // 🔥 THÊM MỚI: Định dạng chống hiển thị kiểu 1e9 cho cột Giá khởi điểm và Giá mua đứt
        setupPriceColumnFormat(colStartPrice);
        setupPriceColumnFormat(colBinPrice);

        setupTimeLeftColumn();
        setupPaymentStatusColumn();
        setupImageColumn();
        ClientManager.getInstance().addUpdateListener(this);
    }

    @Override
    public void onUpdateReceived(String signal) {
        if (signal.equals("REFRESH")){
            refreshAll();
        }
    }

    private void setupColumns() {
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCategory != null) colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colDetails != null) colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colStartPrice != null) colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        if (colBinPrice != null) colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));
    }

    /**
     * 🔥 THÊM MỚI: Hàm format định dạng số tiền cho cột TableView chống lỗi hiển thị 1e9
     */
    private void setupPriceColumnFormat(TableColumn<Item, Double> column) {
        if (column != null) {
            column.setCellFactory(col -> new TableCell<Item, Double>() {
                @Override
                protected void updateItem(Double price, boolean empty) {
                    super.updateItem(price, empty);
                    if (empty || price == null) {
                        setText(null);
                    } else {
                        // Định dạng có dấu phân cách hàng nghìn và thêm chữ VNĐ (Ví dụ: 1,000,000,000 VNĐ)
                        setText(String.format("%,.0f VNĐ", price));
                    }
                }
            });
        }
    }

    /**
     * Hàm nạp ảnh mặc định từ tài nguyên hệ thống (An toàn, chống Crash)
     */
    private Image getDefaultImage() {
        try {
            return new Image(getClass().getResourceAsStream("/images/default.png"));
        } catch (Exception e) {
            try {
                return new Image(getClass().getResourceAsStream("/com/auction/ui/images/default.png"));
            } catch (Exception ex) {
                System.out.println("Lỗi: Không tìm thấy file ảnh default.png trong resources.");
                return null;
            }
        }
    }

    // --- 🚀 ĐÃ SỬA CHÍ MẠNG: Đọc link mạng trực tiếp từ Cloudinary ---
    private void setupImageColumn() {
        if (colImage != null) {
            colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
            colImage.setCellFactory(column -> new TableCell<Item, String>() {
                private final ImageView imageView = new ImageView();

                @Override
                protected void updateItem(String imagePath, boolean empty) {
                    super.updateItem(imagePath, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        imageView.setFitWidth(50);
                        imageView.setFitHeight(50);
                        imageView.setPreserveRatio(true);

                        // Nếu là URL hợp lệ (bắt đầu bằng http từ Cloudinary)
                        if (imagePath != null && !imagePath.isEmpty() && imagePath.startsWith("http")) {
                            // ✅ Tham số 'true' kích hoạt cơ chế Background Loading giúp tự động tải ảnh song song, cuộn bảng mượt mà
                            imageView.setImage(new Image(imagePath, true));
                        } else {
                            // Trường hợp ảnh trống hoặc dính text local cũ ("default.png") -> Ốp ảnh mặc định sạch đẹp
                            imageView.setImage(getDefaultImage());
                        }
                        setGraphic(imageView);
                    }
                }
            });
        }
    }

    private void setupTimeLeftColumn() {
        if (colTimeLeft != null) {
            colTimeLeft.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            colTimeLeft.setCellFactory(column -> new TableCell<Item, Timestamp>() {
                @Override
                protected void updateItem(Timestamp endTime, boolean empty) {
                    super.updateItem(endTime, empty);
                    if (empty || endTime == null) {
                        setText(null); setStyle("");
                    } else {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime end = endTime.toLocalDateTime();
                        if (now.isAfter(end)) {
                            setText("Đã kết thúc"); setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        } else {
                            Duration d = Duration.between(now, end);
                            long days = d.toDays();
                            if (days > 0) setText(String.format("%d ngày %02dh:%02dm", days, d.toHoursPart(), d.toMinutesPart()));
                            else setText(String.format("%02d giờ %02d phút", d.toHoursPart(), d.toMinutesPart()));
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    private void setupPaymentStatusColumn() {
        if (colPayStatus != null) {
            colPayStatus.setCellFactory(column -> new TableCell<Item, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null); setStyle("");
                    } else {
                        Item currentItem = getTableRow().getItem();
                        String status = currentItem.getStatus();
                        String paymentStatus = currentItem.getPaymentStatus();

                        if ("OPEN".equalsIgnoreCase(status) || currentItem.getEndTime() == null) {
                            setText("—"); setStyle("-fx-text-fill: #7f8c8d; -fx-alignment: CENTER;");
                            return;
                        }
                        if ("PAID".equalsIgnoreCase(paymentStatus)) {
                            setText("Đã thanh toán"); setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER;");
                            return;
                        }
                        if ("EXPIRED".equalsIgnoreCase(paymentStatus)) {
                            setText("Bị hủy"); setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                        if ("PENDING".equalsIgnoreCase(paymentStatus)){
                            setText("Chưa thanh toán"); setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                    }
                }
            });
        }
    }

    public void setSellerInfo(int id, String username) {
        this.sellerId = id; this.username = username;
        refreshAll();
    }

    @FXML
    public void refreshAll() {
        if (btnRefresh != null) btnRefresh.setDisable(true);
        new Thread(() -> {
            try {
                double balance = (username != null) ? sellerDAO.getUserBalance(username) : 0;
                ObservableList<Item> items = itemDAO.getItemsBySeller(this.sellerId);
                Platform.runLater(() -> {
                    if (lblBalance != null) lblBalance.setText(String.format("%,.0f VNĐ", balance));
                    if (tableItems != null) tableItems.setItems(items);
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> { if (btnRefresh != null) btnRefresh.setDisable(false); });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private void handleOpenDeposit() {
        openTransactionWindow("DEPOSIT"); ClientManager.getInstance().sendCommand("UPDATE");
    }

    @FXML private void handleOpenWithdraw() {
        openTransactionWindow("WITHDRAW"); ClientManager.getInstance().sendCommand("UPDATE");
    }

    private void openTransactionWindow(String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/deposit_view.fxml"));
            Parent root = loader.load();
            TransactionController controller = loader.getController();
            controller.setUsername(this.username); controller.setMode(mode);
            Stage stage = new Stage(); stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(mode.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền");
            stage.setScene(new Scene(root)); stage.showAndWait();
            refreshAll();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void onAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/add_item.fxml"));
            Parent root = loader.load();
            AddItemController controller = loader.getController();
            controller.setUserId(this.sellerId);
            Stage stage = new Stage(); stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Đăng sản phẩm mới"); stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshAll();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        if (tableItems != null && tableItems.getScene() != null) {
            ClientManager.getInstance().removeUpdateListener(this);
            Stage stage = (Stage) tableItems.getScene().getWindow();
            NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
        }
    }
}
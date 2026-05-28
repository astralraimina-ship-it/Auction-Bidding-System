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
import javafx.scene.image.WritableImage;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
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

    // CACHE ẢNH
    public static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // Tạo sẵn 1 ảnh trống 1x1 pixel trong suốt để làm placeholder trên RAM (Tránh lỗi thiếu file local)
    private final Image placeholderImage = new WritableImage(1, 1);

    @FXML
    public void initialize() {
        setupColumns();
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
        // 🔥 NHẬN DỮ LIỆU ẢNH TỪ SERVER
        else if (signal.startsWith("IMAGE_RESPONSE;")) {
            String[] parts = signal.split(";", 3);
            if (parts.length == 3) {
                String fileName = parts[1];
                String base64 = parts[2];
                try {
                    byte[] decoded = Base64.getDecoder().decode(base64);
                    ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
                    Image img = new Image(bis);
                    imageCache.put(fileName, img); // Cập nhật ảnh thật vào Cache

                    Platform.runLater(() -> tableItems.refresh()); // Vẽ lại bảng
                } catch (Exception e) {
                    System.out.println("Lỗi giải mã ảnh: " + e.getMessage());
                }
            }
        }
    }

    private void setupColumns() {
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCategory != null) colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colDetails != null) colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colStartPrice != null) colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        if (colBinPrice != null) colBinPrice.setCellValueFactory(new PropertyValueFactory<>("binPrice"));
    }

    private void setupImageColumn() {
        if (colImage != null) {
            colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
            colImage.setCellFactory(column -> new TableCell<Item, String>() {
                private final ImageView imageView = new ImageView();

                @Override
                protected void updateItem(String imagePath, boolean empty) {
                    super.updateItem(imagePath, empty);
                    if (empty || imagePath == null || imagePath.isEmpty() || imagePath.equals("default.png")) {
                        setGraphic(null);
                    } else {
                        imageView.setFitWidth(50);
                        imageView.setFitHeight(50);
                        imageView.setPreserveRatio(true);

                        if (imageCache.containsKey(imagePath)) {
                            // Ảnh đã có sẵn
                            imageView.setImage(imageCache.get(imagePath));
                        } else {
                            // Chưa có -> Gửi lệnh tải và gán placeholder
                            imageCache.put(imagePath, placeholderImage);
                            ClientManager.getInstance().sendCommand("GET_IMAGE;" + imagePath);
                            imageView.setImage(placeholderImage);
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
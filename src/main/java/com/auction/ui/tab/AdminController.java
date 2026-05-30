package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.common.user.User;
import com.auction.database.AdminDAO;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import com.auction.transaction.Transaction;
import com.auction.util.NavigationService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AdminController implements ClientManager.UpdateListener {
    @FXML private Label lblBalance;

    // --- Bảng Người dùng ---
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML private TableColumn<User, Double> colUserBalance;

    // --- Bảng Giao dịch ---
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colTransUser, colTransType;
    @FXML private TableColumn<Transaction, Double> colTransAmount, colTransNet;

    // --- Bảng Sản phẩm ---
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName, colCategory, colSeller, colDetails, colTimeLeft;
    @FXML private TableColumn<Item, Double> colStartPrice, colBinPrice;

    // 🔥 CỘT ẢNH MỚI CHO ADMIN
    @FXML private TableColumn<Item, String> colImage;

    @FXML private Button btnRefresh;

    private final AdminDAO adminDAO = new AdminDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        setupColumns();
        refreshData(); // Tự động load lần đầu
        ClientManager.getInstance().addUpdateListener(this);
    }

    @Override
    public void onUpdateReceived(String signal) {
        if (signal.equals("REFRESH")){
            refreshData();
        }
    }

    // Hàm Refresh dùng chung cho cả 3 bảng, chạy ngầm để không treo App
    @FXML
    public void refreshData() {
        System.out.println(">>> ĐANG LẤY DỮ LIỆU MỚI TỪ AIVEN CLOUD...");
        if (btnRefresh != null) btnRefresh.setDisable(true); // Khóa nút tránh bấm liên tục

        // Chạy luồng phụ để kết nối Database
        new Thread(() -> {
            try {
                // Lấy dữ liệu từ Cloud
                ObservableList<User> userList = adminDAO.getAllUsers();
                ObservableList<Transaction> transList = adminDAO.getPendingTransactions();
                ObservableList<Item> itemList = itemDAO.getAllItemsForAdmin();

                // Đẩy dữ liệu về luồng UI sau khi lấy xong
                Platform.runLater(() -> {
                    if (tableUsers != null) tableUsers.setItems(userList);
                    if (tableTransactions != null) tableTransactions.setItems(transList);
                    if (tableItems != null) {
                        tableItems.setItems(itemList);
                        tableItems.refresh();
                    }
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                    System.out.println(">>> ĐÃ CẬP NHẬT TẤT CẢ CÁC BẢNG!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                    showAlert("Lỗi Cloud", "Không thể lấy dữ liệu: " + e.getMessage());
                });
            }
        }).start();
    }

    private void setupColumns() {
        // 1. Users
        if (colUsername != null) colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colRole != null) colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colUserBalance != null) setupUserCurrencyColumn(colUserBalance, "balance");

        // 2. Transactions
        if (colTransUser != null) colTransUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colTransType != null) colTransType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (colTransAmount != null) setupTransCurrencyColumn(colTransAmount, "amount");
        if (colTransNet != null) setupTransCurrencyColumn(colTransNet, "netAmount");

        // 3. Items
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCategory != null) colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colSeller != null) colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        if (colDetails != null) colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colStartPrice != null) setupItemCurrencyColumn(colStartPrice, "startPrice");
        if (colBinPrice != null) setupItemCurrencyColumn(colBinPrice, "binPrice");

        // 🔥 Cấu hình cột hiển thị ảnh cho Admin
        if (colImage != null) {
            setupImageColumn();
        }

        if (colTimeLeft != null) {
            colTimeLeft.setCellFactory(tc -> new TableCell<Item, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    // ĐÃ SỬA: Chống NullPointerException (NPE) khi dữ liệu kết thúc của item bị trống
                    if (empty || getTableRow() == null || getTableRow().getItem() == null || getTableRow().getItem().getEndTime() == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        Item currentItem = getTableRow().getItem();
                        long diff = currentItem.getEndTime().getTime() - System.currentTimeMillis();
                        if (diff <= 0 || "CLOSED".equals(currentItem.getStatus())) {
                            setText("Đã kết thúc");
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        } else {
                            long hours = diff / 3600000;
                            long mins = (diff % 3600000) / 60000;
                            setText(String.format("%02dh %02dm còn lại", hours, mins));
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    /**
     * Hàm lấy ảnh mặc định hệ thống an toàn
     */
    private Image getDefaultImage() {
        try {
            return new Image(getClass().getResourceAsStream("/images/default.png"));
        } catch (Exception e) {
            try {
                return new Image(getClass().getResourceAsStream("/com/auction/ui/images/default.png"));
            } catch (Exception ex) {
                System.out.println("Lỗi: Không tìm thấy file default.png trong resources.");
                return null;
            }
        }
    }

    /**
     * 🔥 HÀM MỚI: Tự động render ảnh từ URL đám mây không chặn luồng UI
     */
    private void setupImageColumn() {
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

                    // Nếu đường dẫn là link URL Cloudinary hợp lệ
                    if (imagePath != null && !imagePath.isEmpty() && imagePath.startsWith("http")) {
                        // Tham số 'true' kích hoạt Background Loading giúp giao diện cuộn mượt
                        imageView.setImage(new Image(imagePath, true));
                    } else {
                        imageView.setImage(getDefaultImage());
                    }
                    setGraphic(imageView);
                }
            }
        });
    }

    private void setupItemCurrencyColumn(TableColumn<Item, Double> c, String p) {
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setCellFactory(tc -> new TableCell<Item, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%,.0f VNĐ", v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void setupUserCurrencyColumn(TableColumn<User, Double> c, String p) {
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setCellFactory(tc -> new TableCell<User, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%,.0f VNĐ", v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void setupTransCurrencyColumn(TableColumn<Transaction, Double> c, String p) {
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setCellFactory(tc -> new TableCell<Transaction, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%,.0f VNĐ", v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    @FXML private void handleApproveTrans() {
        Transaction s = tableTransactions.getSelectionModel().getSelectedItem();
        if (s != null && adminDAO.approveTransaction(s)){
            ClientManager.getInstance().sendCommand("TRANSACTION_UPDATED");
        }
    }

    @FXML private void handleRejectTrans() {
        ClientManager.getInstance().sendCommand("TRANSACTION_UPDATED");
    }

    @FXML private void handleLogout() {
        ClientManager.getInstance().sendCommand("LOGOUT");
        ClientManager.getInstance().removeUpdateListener(this);
        Stage stage = (Stage) tableUsers.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
    }

    @FXML private void handleApproveUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null && "PENDING".equals(selected.getStatus())) {
            if (adminDAO.updateUserStatus(selected.getUsername(), "APPROVED")){
                ClientManager.getInstance().sendCommand("USER_UPDATED;" + selected.getId() + ";APPROVED");
            }
        }
    }

    @FXML private void handleBlockUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn người dùng muốn khóa!");
            return;
        }
        if (adminDAO.updateUserStatus(selected.getUsername(), "BLOCKED")) {
            ClientManager.getInstance().sendCommand("USER_UPDATED;" + selected.getId() + ";BLOCKED");
            refreshData();
        }
    }

    @FXML
    private void handleUnblockUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn người dùng muốn mở khóa!");
            return;
        }

        if ("BLOCKED".equals(selected.getStatus())) {
            if (adminDAO.updateUserStatus(selected.getUsername(), "APPROVED")) {
                ClientManager.getInstance().sendCommand("USER_UPDATED;" + selected.getId() + ";APPROVED");
                refreshData();
            }
        } else {
            showAlert("Thông báo", "Tài khoản này không bị khóa!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
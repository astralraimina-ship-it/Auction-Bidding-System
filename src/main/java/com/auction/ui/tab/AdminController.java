package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.common.user.User;
import com.auction.database.AdminDAO;
import com.auction.database.ItemDAO;
import com.auction.transaction.Transaction;
import com.auction.util.NavigationService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AdminController {
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

    // Thêm nút Button để tiện disable khi đang load
    @FXML private Button btnRefresh;

    private final AdminDAO adminDAO = new AdminDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        setupColumns();
        refreshData(); // Tự động load lần đầu
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

        if (colTimeLeft != null) {
            colTimeLeft.setCellFactory(tc -> new TableCell<Item, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
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

    // Các hàm setup Currency giữ nguyên
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
        if (s != null && adminDAO.approveTransaction(s)) refreshData();
    }

    @FXML private void handleRejectTrans() { refreshData(); }

    @FXML private void handleLogout() {
        Stage stage = (Stage) tableUsers.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
    }

    @FXML private void handleApproveUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null && "PENDING".equals(selected.getStatus())) {
            if (adminDAO.updateUserStatus(selected.getUsername(), "APPROVED")) refreshData();
        }
    }

    @FXML private void handleBlockUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null && adminDAO.updateUserStatus(selected.getUsername(), "BLOCKED")) refreshData();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
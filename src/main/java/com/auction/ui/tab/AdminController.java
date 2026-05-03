package com.auction.ui.tab;

import com.auction.common.item.Item;
import com.auction.common.user.User;
import com.auction.database.AdminDAO;
import com.auction.database.ItemDAO;
import com.auction.transaction.Transaction;
import com.auction.util.NavigationService;
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
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colSeller;
    @FXML private TableColumn<Item, String> colDetails;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colBinPrice;
    @FXML private TableColumn<Item, String> colTimeLeft; // Cột mới thêm

    private final AdminDAO adminDAO = new AdminDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        setupColumns();
        refreshData();
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

        // Xử lý cột Thời gian còn lại / Trạng thái kết thúc
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
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        } else {
                            long hours = diff / 3600000;
                            long mins = (diff % 3600000) / 60000;
                            setText(String.format("%02dh %02dm còn lại", hours, mins));
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                    }
                }
            });
        }
    }

    private void setupItemCurrencyColumn(TableColumn<Item, Double> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setCellFactory(tc -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", value));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                }
            }
        });
    }

    private void setupUserCurrencyColumn(TableColumn<User, Double> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setCellFactory(tc -> new TableCell<User, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%,.0f VNĐ", v));
                setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
            }
        });
    }

    private void setupTransCurrencyColumn(TableColumn<Transaction, Double> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setCellFactory(tc -> new TableCell<Transaction, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%,.0f VNĐ", v));
                setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
            }
        });
    }

    @FXML
    public void refreshData() {
        if (tableUsers != null) tableUsers.setItems(adminDAO.getAllUsers());
        if (tableTransactions != null) tableTransactions.setItems(adminDAO.getPendingTransactions());
        if (tableItems != null) {
            // Sử dụng hàm getAllItemsForAdmin để lấy cả đồ quá hạn
            tableItems.setItems(itemDAO.getAllItemsForAdmin());
            tableItems.refresh();
        }
    }

    @FXML private void handleApproveTrans() {
        Transaction s = tableTransactions.getSelectionModel().getSelectedItem();
        if (s != null && adminDAO.approveTransaction(s)) refreshData();
    }

    @FXML private void handleRejectTrans() { refreshData(); }

    @FXML private void handleLogout() {
        Stage stage = (Stage) lblBalance.getScene().getWindow();
        NavigationService.navigate(stage, "/com/auction/ui/login.fxml", "UET Auction - Đăng nhập");
    }
    @FXML
    private void handleApproveUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if ("PENDING".equals(selected.getStatus())) {
                // Gọi DAO để update status thành APPROVED trong database
                if (adminDAO.updateUserStatus(selected.getUsername(), "APPROVED")) {
                    refreshData(); // Load lại bảng để thấy thay đổi
                }
            } else {
                showAlert("Thông báo", "Người dùng này đã được duyệt hoặc đang ở trạng thái khác.");
            }
        } else {
            showAlert("Lỗi", "Vui lòng chọn một người dùng từ danh sách!");
        }
    }

    @FXML
    private void handleBlockUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (adminDAO.updateUserStatus(selected.getUsername(), "BLOCKED")) {
                refreshData();
            }
        }
    }

    // Hàm hỗ trợ hiện thông báo nhanh
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
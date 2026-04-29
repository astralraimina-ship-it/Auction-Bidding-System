package com.auction.ui;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {
    // Các thành phần giao diện
    @FXML private Label lblBalance;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML private TableColumn<User, Double> colUserBalance;
    @FXML private ComboBox<String> comboFilter;

    // Dữ liệu và DAO
    private UserDAO userDAO = new UserDAO();
    private String currentUsername;
    private ObservableList<User> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("Dashboard Admin đang khởi tạo...");

        // 1. Cấu hình bảng và các cột (tô màu cho trạng thái)
        setupUserTable();

        // 2. Cấu hình bộ lọc ComboBox
        if (comboFilter != null) {
            comboFilter.getItems().addAll("Tất cả người dùng", "Đang chờ duyệt (PENDING)");
            comboFilter.getSelectionModel().selectFirst();
        }

        // 3. Load dữ liệu ban đầu từ Database
        loadInitialData();
    }

    private void setupUserTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // Custom Cell để tô màu cho cột Trạng thái
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PENDING".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Đỏ đậm
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Xanh lá
                    }
                }
            }
        });
    }

    private void loadInitialData() {
        masterData = userDAO.getAllUsers();
        tableUsers.setItems(masterData);
        System.out.println("Đã nạp thành công " + masterData.size() + " users.");
    }

    // Xử lý khi chọn bộ lọc trên ComboBox
    @FXML
    private void handleFilterChange() {
        String selected = comboFilter.getValue();
        if ("Đang chờ duyệt (PENDING)".equals(selected)) {
            FilteredList<User> filteredData = new FilteredList<>(masterData,
                    user -> "PENDING".equalsIgnoreCase(user.getStatus()));
            tableUsers.setItems(filteredData);
        } else {
            tableUsers.setItems(masterData);
        }
    }

    public void setUserData(String username, String role) {
        this.currentUsername = username;

        // Nếu là Admin thì ẩn phần Mode/Balance cho đẹp
        if ("ADMIN".equalsIgnoreCase(role)) {
            lblBalance.setVisible(false);
            lblBalance.setManaged(false); // Co layout lại
        } else {
            refreshBalance();
        }
    }

    private void refreshBalance() {
        if (currentUsername != null && lblBalance != null) {
            double balance = userDAO.getUserBalance(currentUsername);
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
            lblBalance.setVisible(true);
            lblBalance.setManaged(true);
        }
    }

    @FXML
    private void handleApproveUser() {
        User selectedUser = tableUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert("Thông báo", "Vui lòng chọn một người dùng từ danh sách!");
            return;
        }

        if (!"ADMIN".equalsIgnoreCase(selectedUser.getRole())) {
            showAlert("Lỗi", "Nút này chỉ dùng để duyệt cho tài khoản ADMIN!");
            return;
        }

        if ("APPROVED".equalsIgnoreCase(selectedUser.getStatus())) {
            showAlert("Thông báo", "Tài khoản này đã được duyệt trước đó.");
            return;
        }

        if (userDAO.approveUser(selectedUser.getUsername())) {
            System.out.println("Duyệt thành công: " + selectedUser.getUsername());
            // Cập nhật lại dữ liệu
            loadInitialData();
            handleFilterChange(); // Giữ nguyên bộ lọc hiện tại
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng nhập");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
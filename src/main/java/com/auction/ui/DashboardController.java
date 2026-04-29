package com.auction.ui;

import com.auction.common.user.User;
import com.auction.database.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn; // Thêm import này
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {
    @FXML
    private Label lblBalance;
    @FXML
    private TableView<User> tableUsers; // Đổi tên cho khớp logic
    @FXML
    private TableColumn<User, String> colUsername, colRole, colStatus;
    @FXML
    private TableColumn<User, Double> colUserBalance;

    private UserDAO userDAO = new UserDAO();
    private String currentUsername;

    // CHỈ GIỮ 1 HÀM INITIALIZE DUY NHẤT
    @FXML
    public void initialize() {
        System.out.println("Dashboard đang khởi tạo...");

        // 1. Cấu hình cột (Phải khớp với tên biến trong class User)
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // 2. Lấy dữ liệu từ DAO và đưa vào bảng
        ObservableList<User> userList = userDAO.getAllUsers();

        if (userList != null && !userList.isEmpty()) {
            tableUsers.setItems(userList);
            System.out.println("Đã nạp thành công " + userList.size() + " users.");
        } else {
            System.out.println("Cảnh báo: Không có dữ liệu user nào từ Database!");
        }
    }

    private void setupUserTable() {
        // Lưu ý: Các chuỗi này PHẢI khớp với getter trong class User (ví dụ: getUsername -> "username")
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
    }

    public void setUserData(String username) {
        this.currentUsername = username;
        refreshBalance();
    }

    private void refreshBalance() {
        if (lblBalance != null && currentUsername != null) {
            double balance = userDAO.getUserBalance(currentUsername);
            lblBalance.setText(String.format("%,.0f VNĐ", balance));
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // 1. Load file login.fxml
            // Đảm bảo đường dẫn này đúng với cấu trúc thư mục của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/login.fxml"));
            Parent root = loader.load();

            // 2. Lấy Stage hiện tại một cách an toàn
            Stage stage;
            if (tableUsers != null && tableUsers.getScene() != null) {
                stage = (Stage) tableUsers.getScene().getWindow();
            } else {
                stage = (Stage) lblBalance.getScene().getWindow();
            }

            // 3. Chuyển cảnh
            stage.setScene(new Scene(root));
            stage.setTitle("UET Auction - Đăng nhập");
            stage.show();

            System.out.println("Đã đăng xuất tài khoản!");
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
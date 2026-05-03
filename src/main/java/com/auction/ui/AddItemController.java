package com.auction.ui;

import com.auction.common.item.Item;
import com.auction.common.item.ItemFactory;
import com.auction.database.ItemDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.sql.Timestamp;

public class AddItemController {
    // --- Các trường dữ liệu chung ---
    @FXML private TextField txtName, txtStartPrice, txtBinPrice, txtStep;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboDuration; // MỚI THÊM

    // --- Các trường dữ liệu riêng ---
    @FXML private TextField txtBrand, txtState, txtEngineType, txtMileage, txtArtist, txtWarranty;

    @FXML private Button btnCancel;

    private int currentUserId;

    @FXML
    public void initialize() {
        // 1. Khởi tạo danh sách loại hàng
        comboCategory.getItems().addAll("VEHICLE", "ART", "ELECTRONICS", "OTHER");
        comboCategory.setValue("OTHER");

        // 2. Khởi tạo danh sách thời lượng đấu giá
        comboDuration.getItems().addAll("1 giờ", "5 giờ", "12 giờ", "24 giờ", "3 ngày", "7 ngày");
        comboDuration.setValue("1 giờ");

        // 3. Lắng nghe thay đổi loại hàng để ẩn/hiện các ô nhập liệu chi tiết
        comboCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFieldState(newValue);
        });

        // Chạy lần đầu để setup trạng thái mặc định cho "OTHER"
        updateFieldState("OTHER");
    }

    // Hàm logic ẩn/hiện/disable các trường thông tin chi tiết
    private void updateFieldState(String category) {
        // Reset: Disable tất cả trước
        txtBrand.setDisable(true);
        txtState.setDisable(true);
        txtEngineType.setDisable(true);
        txtMileage.setDisable(true);
        txtArtist.setDisable(true);
        txtWarranty.setDisable(true);

        if ("VEHICLE".equals(category)) {
            txtBrand.setDisable(false);
            txtState.setDisable(false);
            txtEngineType.setDisable(false);
            txtMileage.setDisable(false);
        } else if ("ART".equals(category)) {
            txtArtist.setDisable(false);
            txtState.setDisable(false);
        } else if ("ELECTRONICS".equals(category)) {
            txtBrand.setDisable(false);
            txtWarranty.setDisable(false);
            txtState.setDisable(false);
        }
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
    }

    @FXML
    private void handleSave() {
        try {
            // Kiểm tra nhập liệu cơ bản
            if (txtName.getText().isEmpty() || txtStartPrice.getText().isEmpty()) {
                showAlert("Thông báo", "Vui lòng điền tên sản phẩm và giá khởi điểm!");
                return;
            }

            // 1. Xử lý thời gian kết thúc
            String selectedDuration = comboDuration.getValue();
            int hours = 0;
            if (selectedDuration.contains("giờ")) {
                hours = Integer.parseInt(selectedDuration.replace(" giờ", ""));
            } else if (selectedDuration.contains("ngày")) {
                hours = Integer.parseInt(selectedDuration.replace(" ngày", "")) * 24;
            }

            LocalDateTime endLDT = LocalDateTime.now().plusHours(hours);
            Timestamp endTime = Timestamp.valueOf(endLDT);

            // 2. Thu thập dữ liệu chung
            Map<String, Object> commonData = new HashMap<>();
            commonData.put("id", 0);
            commonData.put("name", txtName.getText().trim());
            commonData.put("description", txtDescription.getText().trim());
            commonData.put("startPrice", Double.parseDouble(txtStartPrice.getText()));
            commonData.put("binPrice", Double.parseDouble(txtBinPrice.getText()));
            commonData.put("step", Double.parseDouble(txtStep.getText()));
            commonData.put("endTime", endTime); // LƯU THỜI GIAN
            commonData.put("status", "OPEN");   // TRẠNG THÁI MẶC ĐỊNH

            // 3. Thu thập dữ liệu riêng
            String category = comboCategory.getValue();
            Map<String, Object> specificData = new HashMap<>();
            specificData.put("brand", getSafeText(txtBrand));
            specificData.put("state", getSafeText(txtState));
            specificData.put("engineType", getSafeText(txtEngineType));
            specificData.put("artist", getSafeText(txtArtist));
            specificData.put("warranty", getSafeText(txtWarranty));
            specificData.put("mileage", parseDoubleSafe(txtMileage));

            // 4. Tạo Object và lưu
            Item newItem = ItemFactory.createItem(category, commonData, specificData);
            ItemDAO dao = new ItemDAO();
            if (dao.addItem(newItem, currentUserId)) {
                showAlert("Thành công", "Đã đăng sản phẩm! Hết hạn lúc: " + endLDT.toString());
                closeWindow();
            } else {
                showAlert("Lỗi", "Không thể lưu sản phẩm vào Database!");
            }

        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Giá tiền, bước giá phải là số!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    private String getSafeText(TextField field) {
        return (field != null && !field.isDisabled() && field.getText() != null) ? field.getText().trim() : "";
    }

    private double parseDoubleSafe(TextField field) {
        try {
            if (field == null || field.isDisabled() || field.getText().isEmpty()) return 0.0;
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return 0.0;
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
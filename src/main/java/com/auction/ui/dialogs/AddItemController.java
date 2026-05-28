package com.auction.ui.dialogs;

import com.auction.common.item.Item;
import com.auction.common.item.ItemFactory;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

public class AddItemController {
    // --- Các trường dữ liệu chung ---
    @FXML private TextField txtName, txtStartPrice, txtBinPrice, txtStep;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboDuration;

    // --- Các trường dữ liệu riêng ---
    @FXML private TextField txtBrand, txtState, txtEngineType, txtMileage, txtArtist, txtWarranty;

    // 🔥 THÊM MỚI: Khung hiển thị ảnh xem trước (Preview)
    @FXML private ImageView imgPreview;

    private int currentUserId; // Đây chính là sellerId

    // 🔥 THÊM MỚI: Các biến lưu thông tin ảnh tạm thời
    private File selectedImageFile = null;
    private String finalImageName = "default.png"; // Mặc định nếu không chọn ảnh

    @FXML
    public void initialize() {
        // 1. Khởi tạo danh sách loại hàng
        comboCategory.getItems().addAll("VEHICLE", "ART", "ELECTRONICS", "OTHER");
        comboCategory.setValue("OTHER");

        // 2. Khởi tạo danh sách thời lượng đấu giá
        comboDuration.getItems().addAll("1 giờ", "5 giờ", "12 giờ", "24 giờ", "3 ngày", "7 ngày");
        comboDuration.setValue("1 giờ");

        // 3. Lắng nghe thay đổi loại hàng để ẩn/hiện trường chi tiết
        comboCategory.valueProperty().addListener((obs, oldVal, newVal) -> updateFieldState(newVal));
        updateFieldState("OTHER");
    }

    private void updateFieldState(String category) {
        // Vô hiệu hóa tất cả trước khi mở lại theo loại
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

    // 🔥 THÊM MỚI: Sự kiện nút "Chọn ảnh sản phẩm"
    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) txtName.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            // Tạo tên file ngẫu nhiên bằng UUID kết hợp đuôi mở rộng cũ để tránh trùng lặp trên Server
            String ext = file.getName().substring(file.getName().lastIndexOf("."));
            this.finalImageName = UUID.randomUUID().toString() + ext;

            // Hiển thị ảnh xem trước lên ImageView
            try (FileInputStream fis = new FileInputStream(file)) {
                Image img = new Image(fis);
                if (imgPreview != null) {
                    imgPreview.setImage(img);
                }
            } catch (Exception e) {
                System.out.println("Lỗi hiển thị ảnh xem trước: " + e.getMessage());
            }
        }
    }

    // Quan trọng: Phải gọi hàm này từ SellerController khi mở Dialog
    public void setUserId(int userId) {
        this.currentUserId = userId;
    }

    @FXML
    private void handleSave() {
        try {
            // Validate dữ liệu bắt buộc
            if (txtName.getText().trim().isEmpty() || txtStartPrice.getText().trim().isEmpty()) {
                showAlert("Thông báo", "Tên sản phẩm và Giá khởi điểm không được để trống!");
                return;
            }

            double stepPrice = parseDoubleSafe(txtStep);
            if (stepPrice <= 0 || stepPrice % 10000 != 0) {
                showAlert("Thông báo", "Bước giá phải lớn hơn 0 và là bội số của 10,000 VNĐ (Ví dụ: 10,000, 20,000, 50,000...)!");
                return;
            }

            // 1. Xử lý thời gian kết thúc (endTime)
            String selectedDuration = comboDuration.getValue();
            int hours = 0;
            if (selectedDuration.contains("giờ")) {
                hours = Integer.parseInt(selectedDuration.replace(" giờ", ""));
            } else if (selectedDuration.contains("ngày")) {
                hours = Integer.parseInt(selectedDuration.replace(" ngày", "")) * 24;
            }
            Timestamp endTime = Timestamp.valueOf(LocalDateTime.now().plusHours(hours));

            // 2. Thu thập dữ liệu chung (Map key phải khớp với ItemDAO)
            Map<String, Object> commonData = new HashMap<>();
            commonData.put("id", 0);
            commonData.put("name", txtName.getText().trim());
            commonData.put("category", comboCategory.getValue());
            commonData.put("description", txtDescription.getText().trim());
            commonData.put("startPrice", parseDoubleSafe(txtStartPrice));
            commonData.put("binPrice", parseDoubleSafe(txtBinPrice));
            commonData.put("step", parseDoubleSafe(txtStep));
            commonData.put("endTime", endTime);
            commonData.put("status", "OPEN");

            // 🔥 THÊM MỚI: Truyền đường dẫn/tên file ảnh vào dữ liệu chung cho Factory tạo đối tượng
            commonData.put("imagePath", finalImageName);

            // 3. Thu thập dữ liệu riêng
            Map<String, Object> specificData = new HashMap<>();
            specificData.put("brand", getSafeText(txtBrand));
            specificData.put("state", getSafeText(txtState));
            specificData.put("engineType", getSafeText(txtEngineType));
            specificData.put("artist", getSafeText(txtArtist));
            specificData.put("warranty", getSafeText(txtWarranty));
            specificData.put("mileage", parseDoubleSafe(txtMileage));

            // 4. Tạo Object qua Factory và lưu vào DB
            Item newItem = ItemFactory.createItem(comboCategory.getValue(), commonData, specificData);
            ItemDAO dao = new ItemDAO();

            // Kiểm tra sellerId trước khi gửi
            if (currentUserId <= 0) {
                showAlert("Lỗi hệ thống", "Không tìm thấy ID người bán. Vui lòng đăng nhập lại!");
                return;
            }

            if (dao.addItem(newItem, currentUserId)) {
                // 🔥 THÊM MỚI: Nếu người dùng có chọn ảnh, tiến hành tải ảnh lên Server thông qua luồng Socket mạng
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    try {
                        byte[] fileBytes = Files.readAllBytes(selectedImageFile.toPath());
                        String base64String = Base64.getEncoder().encodeToString(fileBytes);

                        // Định dạng chuỗi lệnh gửi: UPLOAD_IMAGE;tên_file;chuỗi_base64
                        String uploadCmd = "UPLOAD_IMAGE;" + finalImageName + ";" + base64String;
                        ClientManager.getInstance().sendCommand(uploadCmd);
                    } catch (Exception imgEx) {
                        System.out.println("Lỗi chuyển đổi/gửi ảnh lên Server: " + imgEx.getMessage());
                    }
                }

                int itemId = newItem.getId();
                double startPrice = newItem.getStartPrice();
                double step = newItem.getStep();
                double binPrice = newItem.getBinPrice();

                ClientManager.getInstance().sendCommand("NEW_ITEM;" + itemId + ";" + startPrice + ";" + step + ";" + binPrice);
                showAlert("Thành công", "Sản phẩm đã được đăng lên hệ thống!");
                closeWindow();
            } else {
                showAlert("Lỗi", "Database từ chối lưu sản phẩm. Vui lòng kiểm tra lại kết nối!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if (txtName.getScene() != null) {
            ((Stage) txtName.getScene().getWindow()).close();
        }
    }

    private String getSafeText(TextField field) {
        return (field != null && !field.isDisabled() && field.getText() != null) ? field.getText().trim() : "";
    }

    private double parseDoubleSafe(TextField field) {
        try {
            String text = field.getText().trim();
            return text.isEmpty() ? 0.0 : Double.parseDouble(text);
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
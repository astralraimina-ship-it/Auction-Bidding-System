package com.auction.ui.dialogs;

import com.auction.common.item.Item;
import com.auction.common.item.ItemFactory;
import com.auction.database.ItemDAO;
import com.auction.network.ClientManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Import thư viện Cloudinary
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;

public class AddItemController {
    // --- Các trường dữ liệu chung ---
    @FXML private TextField txtName, txtStartPrice, txtBinPrice, txtStep;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboDuration;

    // --- Các trường dữ liệu riêng ---
    @FXML private TextField txtBrand, txtState, txtEngineType, txtMileage, txtArtist, txtWarranty;

    // Khung hiển thị ảnh xem trước (Preview)
    @FXML private ImageView imgPreview;

    private int currentUserId; // Đây chính là sellerId

    // Biến lưu thông tin file ảnh người dùng chọn ở máy local
    private File selectedImageFile = null;

    // ⚠️ ĐÃ THAY ĐỔI: Cấu hình tài khoản Cloudinary của ông tại đây
    private final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dvoz4wgqh",   // Thay bằng Cloud Name của ông
            "api_key", "582996547526317",         // Thay bằng API Key của ông
            "api_secret", "cxXICxcTG46C4ezTOulnlprAIdQ"    // Thay bằng API Secret của ông
    ));

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

    // Sự kiện nút "Chọn ảnh sản phẩm"
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

            // Hiển thị ảnh xem trước lên ImageView local
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

    public void setUserId(int userId) {
        this.currentUserId = userId;
    }

    @FXML
    private void handleSave() {
        // Chạy ngầm xử lý lưu & upload ảnh để tránh làm đơ/vỡ giao diện JavaFX (UI Thread)
        new Thread(() -> {
            try {
                // Validate dữ liệu bắt buộc (Đẩy về chạy trên UI Thread bằng Platform.runLater nếu cần hiện Alert)
                if (txtName.getText().trim().isEmpty() || txtStartPrice.getText().trim().isEmpty()) {
                    Platform.runLater(() -> showAlert("Thông báo", "Tên sản phẩm và Giá khởi điểm không được để trống!"));
                    return;
                }

                double stepPrice = parseDoubleSafe(txtStep);
                if (stepPrice <= 0 || stepPrice % 10000 != 0) {
                    Platform.runLater(() -> showAlert("Thông báo", "Bước giá phải lớn hơn 0 và là bội số của 10,000 VNĐ!"));
                    return;
                }

                // Kiểm tra sellerId trước khi gửi
                if (currentUserId <= 0) {
                    Platform.runLater(() -> showAlert("Lỗi hệ thống", "Không tìm thấy ID người bán. Vui lòng đăng nhập lại!"));
                    return;
                }

                // ĐÃ THAY ĐỔI: Tiến hành đẩy ảnh lên Cloudinary để lấy link URL tuyệt đối trước
                String finalImageUrl = ""; // Mặc định chuỗi rỗng nếu không chọn ảnh
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    try {
                        System.out.println(">>> Đang đẩy ảnh lên Cloudinary...");
                        Map uploadResult = cloudinary.uploader().upload(selectedImageFile, ObjectUtils.emptyMap());
                        finalImageUrl = (String) uploadResult.get("url"); // Lấy link dạng https://res.cloudinary.com/...
                        System.out.println(">>> Upload thành công! URL ảnh: " + finalImageUrl);
                    } catch (Exception cloudEx) {
                        System.out.println(">>> Lỗi upload Cloudinary: " + cloudEx.getMessage());
                        Platform.runLater(() -> showAlert("Lỗi upload ảnh", "Không thể tải ảnh lên hệ thống Cloud. Vui lòng thử lại!"));
                        return;
                    }
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

                // 2. Thu thập dữ liệu chung (Lưu URL ảnh trực tiếp vào trường imagePath)
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

                // ✅ THAY ĐỔI CHÍ MẠNG: Gán thẳng link URL mạng vào thay cho tên file local cũ
                commonData.put("imagePath", finalImageUrl);

                // 3. Thu thập dữ liệu riêng
                Map<String, Object> specificData = new HashMap<>();
                specificData.put("brand", getSafeText(txtBrand));
                specificData.put("state", getSafeText(txtState));
                specificData.put("engineType", getSafeText(txtEngineType));
                specificData.put("artist", getSafeText(txtArtist));
                specificData.put("warranty", getSafeText(txtWarranty));
                specificData.put("mileage", parseDoubleSafe(txtMileage));

                // 4. Tạo Object qua Factory và lưu vào DB trung tâm
                Item newItem = ItemFactory.createItem(comboCategory.getValue(), commonData, specificData);
                ItemDAO dao = new ItemDAO();

                if (dao.addItem(newItem, currentUserId)) {
                    int itemId = newItem.getId();
                    double startPrice = newItem.getStartPrice();
                    double step = newItem.getStep();
                    double binPrice = newItem.getBinPrice();

                    // ✅ ĐÃ XÓA SẠCH: Đoạn code gửi Socket UPLOAD_IMAGE Base64 cũ biến mất hoàn toàn ở đây!

                    // Chỉ gửi thông báo phòng đấu giá mới cho các máy khác cập nhật giao diện
                    ClientManager.getInstance().sendCommand("NEW_ITEM;" + itemId + ";" + startPrice + ";" + step + ";" + binPrice);

                    Platform.runLater(() -> {
                        showAlert("Thành công", "Sản phẩm đã được đăng lên hệ thống!");
                        closeWindow();
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi", "Database từ chối lưu sản phẩm. Vui lòng kiểm tra lại kết nối!"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi", "Đã xảy ra lỗi: " + e.getMessage()));
            }
        }).start();
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
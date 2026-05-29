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

    // Các Button để điều khiển trạng thái (Chống click đúp liên tục)
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private int currentUserId; // Đây chính là sellerId

    // Biến lưu thông tin file ảnh người dùng chọn ở máy local
    private File selectedImageFile = null;

    // Cấu hình tài khoản Cloudinary
    private final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dvoz4wgqh",
            "api_key", "582996547526317",
            "api_secret", "cxXICxcTG46C4ezTOulnlprAIdQ"
    ));

    @FXML
    public void initialize() {
        comboCategory.getItems().addAll("VEHICLE", "ART", "ELECTRONICS", "OTHER");
        comboCategory.setValue("OTHER");

        comboDuration.getItems().addAll("1 giờ", "5 giờ", "12 giờ", "24 giờ", "3 ngày", "7 ngày");
        comboDuration.setValue("1 giờ");

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

    /**
     * 🔥 THÊM MỚI: Hàm hỗ trợ tự động dọn dẹp các ký tự dấu chấm (.) và dấu phẩy (,)
     * để tránh việc lỗi ép kiểu sai lệch hàng nghìn sang số thập phân trong Java.
     */
    private String cleanNumberStr(String str) {
        if (str == null) return "";
        return str.replace(".", "").replace(",", "").trim();
    }

    @FXML
    private void handleSave() {
        // 1. Khóa các nút điều hướng ngay lập tức trên UI Thread để chống click đúp
        if (btnSave != null) btnSave.setDisable(true);
        if (btnCancel != null) btnCancel.setDisable(true);

        // 2. Lấy dữ liệu chữ thô từ giao diện
        String name = txtName.getText().trim();
        String description = txtDescription.getText().trim();
        String category = comboCategory.getValue();
        String duration = comboDuration.getValue();

        String brand = getSafeText(txtBrand);
        String state = getSafeText(txtState);
        String engineType = getSafeText(txtEngineType);
        String artist = getSafeText(txtArtist);
        String warranty = getSafeText(txtWarranty);

        // 3. Tiến hành làm sạch chuỗi số (Xóa bỏ hoàn toàn dấu . và , phân cách)
        String startPriceStr = cleanNumberStr(txtStartPrice.getText());
        String stepStr = cleanNumberStr(txtStep.getText());
        String binPriceStr = cleanNumberStr(txtBinPrice.getText());
        String mileageStr = txtMileage.getText() != null ? cleanNumberStr(txtMileage.getText()) : "";

        // =========================================================================
        // 🔥 ĐÃ CẢI TIẾN: TOÀN BỘ LOGIC KIỂM TRA (VALIDATE) ĐƯỢC THỰC HIỆN TRÊN UI THREAD
        // =========================================================================

        // Kiểm tra trống các trường bắt buộc
        if (name.isEmpty() || startPriceStr.isEmpty() || stepStr.isEmpty()) {
            showAlert("Thông báo", "Tên sản phẩm, Giá khởi điểm và Bước giá không được để trống!");
            enableButtons();
            return;
        }

        // Kiểm tra định dạng số hợp lệ
        if (!isNumeric(startPriceStr) || !isNumeric(stepStr)) {
            showAlert("Thông báo lỗi nhập liệu", "Giá khởi điểm và Bước giá bắt buộc phải là một số nguyên hợp lệ!");
            enableButtons();
            return;
        }
        if (!binPriceStr.isEmpty() && !isNumeric(binPriceStr)) {
            showAlert("Thông báo lỗi nhập liệu", "Giá mua đứt bắt buộc phải là một số nguyên hợp lệ (hoặc để trống)!");
            enableButtons();
            return;
        }

        // Ép kiểu số an toàn tuyệt đối sau khi chuỗi đã được dọn sạch ký tự phân cách
        double startPrice = Double.parseDouble(startPriceStr);
        double stepPrice = Double.parseDouble(stepStr);
        double binPrice = binPriceStr.isEmpty() ? 0.0 : Double.parseDouble(binPriceStr);
        double mileage = 0.0;
        System.out.println(startPrice);
        System.out.println(stepPrice);
        System.out.println(binPrice);

        if ("VEHICLE".equals(category) && !mileageStr.isEmpty()) {
            if (!isNumeric(mileageStr)) {
                showAlert("Thông báo lỗi", "Số km đã đi (Mileage) phải là một số hợp lệ!");
                enableButtons();
                return;
            }
            mileage = Double.parseDouble(mileageStr);
        }

        // Kiểm tra logic toán học của các mức giá tiền
        if (startPrice <= 0) {
            showAlert("Thông báo toán học", "Giá khởi điểm bắt buộc phải lớn hơn 0 VNĐ!");
            enableButtons();
            return;
        }

        if (stepPrice <= 0 || stepPrice % 10000 != 0) {
            showAlert("Thông báo toán học", "Bước giá phải lớn hơn 0 và là bội số của 10,000 VNĐ (Ví dụ: 10000, 20000, 50000...)!");
            enableButtons();
            return;
        }


        // 🔥 KHÓA CHẶT CHẼ: Kiểm tra logic chặn lỗi Giá mua đứt nhỏ hơn hoặc bằng Giá khởi điểm
        if (binPrice >= 0) {
            double giaToiThieuPhaiDat = startPrice + stepPrice;
            if (binPrice < giaToiThieuPhaiDat) {
                showAlert("Thông báo lỗi logic giá",
                        "Giá bán đứt không hợp lệ!\n\n" +
                                "Quy định hệ thống: Giá bán đứt phải lớn hơn hoặc bằng: [Giá khởi điểm + Bước giá].\n" +
                                "Mức giá bán đứt tối thiểu hợp lệ cho sản phẩm này phải từ: " + String.format("%,.0f VNĐ", giaToiThieuPhaiDat));
                enableButtons();
                return; // Chặn đứng ngay tại Luồng chính, không bao giờ tạo Thread chạy xuống Database!
            }
        }

        if (currentUserId <= 0) {
            showAlert("Lỗi hệ thống", "Không tìm thấy ID người bán. Vui lòng đăng nhập lại!");
            enableButtons();
            return;
        }

        // =========================================================================
        // CHỈ KHI DỮ LIỆU ĐÃ HỢP LỆ 100% -> MỚI KHỞI TẠO LUỒNG NGẦM ĐỂ XỬ LÝ MẠNG/DATABASE
        // =========================================================================
        final double finalMileage = mileage;
        new Thread(() -> {
            try {
                // Đẩy ảnh lên Cloudinary
                String finalImageUrl = "default.png";
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    try {
                        Map uploadResult = cloudinary.uploader().upload(selectedImageFile, ObjectUtils.emptyMap());
                        finalImageUrl = (String) uploadResult.get("url");
                    } catch (Exception cloudEx) {
                        Platform.runLater(() -> {
                            showAlert("Lỗi mạng", "Không thể tải ảnh lên hệ thống Cloud. Vui lòng kiểm tra internet!");
                            enableButtons();
                        });
                        return;
                    }
                }

                // Xử lý tính toán thời gian kết thúc (endTime)
                int hours = duration.contains("giờ") ?
                        Integer.parseInt(duration.replace(" giờ", "")) :
                        Integer.parseInt(duration.replace(" ngày", "")) * 24;
                Timestamp endTime = Timestamp.valueOf(LocalDateTime.now().plusHours(hours));

                // Thu thập dữ liệu chung
                Map<String, Object> commonData = new HashMap<>();
                commonData.put("id", 0);
                commonData.put("name", name);
                commonData.put("category", category);
                commonData.put("description", description);
                commonData.put("startPrice", startPrice);
                commonData.put("binPrice", binPrice);
                commonData.put("step", stepPrice);
                commonData.put("endTime", endTime);
                commonData.put("status", "OPEN");
                commonData.put("imagePath", finalImageUrl);

                // Thu thập dữ liệu riêng
                Map<String, Object> specificData = new HashMap<>();
                specificData.put("brand", brand);
                specificData.put("state", state);
                specificData.put("engineType", engineType);
                specificData.put("artist", artist);
                specificData.put("warranty", warranty);
                specificData.put("mileage", finalMileage);

                // Lưu vào database trung tâm
                Item newItem = ItemFactory.createItem(category, commonData, specificData);
                ItemDAO dao = new ItemDAO();

                if (dao.addItem(newItem, currentUserId)) {
                    int itemId = newItem.getId();

                    // Phát lệnh socket thông báo phòng đấu giá mới cho các Client khác
                    ClientManager.getInstance().sendCommand("NEW_ITEM;" + itemId + ";" + startPrice + ";" + stepPrice + ";" + binPrice);

                    Platform.runLater(() -> {
                        showAlert("Thành công", "Sản phẩm đã được đăng lên hệ thống đấu giá công khai!");
                        closeWindow();
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Lỗi", "Cơ sở dữ liệu từ chối lưu sản phẩm này!");
                        enableButtons();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
                    enableButtons();
                });
            }
        }).start();
    }

    /**
     * 🔥 CẢI TIẾN THÀNH AN TOÀN LUỒNG (Thread-safe):
     * Tự động nhận diện môi trường luồng để điều khiển UI chính xác, tránh crash giao diện.
     */
    private void enableButtons() {
        if (Platform.isFxApplicationThread()) {
            if (btnSave != null) btnSave.setDisable(false);
            if (btnCancel != null) btnCancel.setDisable(false);
        } else {
            Platform.runLater(() -> {
                if (btnSave != null) btnSave.setDisable(false);
                if (btnCancel != null) btnCancel.setDisable(false);
            });
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

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            double d = Double.parseDouble(str);
            return d >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 🔥 CẢI TIẾN THÀNH AN TOÀN LUỒNG (Thread-safe)
     */
    private void showAlert(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
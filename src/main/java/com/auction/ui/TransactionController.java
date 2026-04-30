package com.auction.ui;

import com.auction.database.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class TransactionController {
    @FXML private TextField txtAmount;
    @FXML private Label lblStatus, lblFeeNote;
    @FXML private Text txtTitle;
    @FXML private Button btnSubmit;

    private UserDAO userDAO = new UserDAO();
    private String currentUsername;
    private String mode = "DEPOSIT"; // Mặc định là nạp

    public void setUsername(String username) {
        this.currentUsername = username;
    }

    // Hàm quan trọng để phân biệt Nạp hay Rút
    public void setMode(String mode) {
        this.mode = mode;
        if ("WITHDRAW".equals(mode)) {
            txtTitle.setText("RÚT TIỀN");
            btnSubmit.setText("Gửi yêu cầu rút tiền");
            btnSubmit.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
            if (lblFeeNote != null) lblFeeNote.setVisible(true);
        } else {
            txtTitle.setText("NẠP TIỀN");
            btnSubmit.setText("Gửi yêu cầu nạp tiền");
            btnSubmit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            if (lblFeeNote != null) lblFeeNote.setVisible(false);
        }
    }

    @FXML
    private void handleTransaction() {
        try {
            String amountStr = txtAmount.getText().trim();
            if (amountStr.isEmpty()) {
                lblStatus.setText("Vui lòng nhập số tiền!");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            if (amount < 10000) {
                lblStatus.setText("Số tiền tối thiểu là 10.000 VNĐ");
                return;
            }

            boolean success = false;
            if ("WITHDRAW".equals(mode)) {
                // Kiểm tra số dư trước khi rút
                double balance = userDAO.getUserBalance(currentUsername);
                if (amount > balance) {
                    lblStatus.setText("Số dư không đủ để rút!");
                    return;
                }
                // Gọi hàm rút tiền (tự tính phí 10% trong DAO)
                success = userDAO.requestWithdraw(currentUsername, amount);
            } else {
                // Gọi hàm nạp tiền
                success = userDAO.requestDeposit(currentUsername, amount);
            }

            if (success) {
                System.out.println("Yêu cầu " + mode + " thành công cho: " + currentUsername);
                closeWindow();
            } else {
                lblStatus.setText("Lỗi hệ thống, vui lòng thử lại!");
            }

        } catch (NumberFormatException e) {
            lblStatus.setText("Số tiền không hợp lệ!");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) txtAmount.getScene().getWindow();
        stage.close();
    }
}
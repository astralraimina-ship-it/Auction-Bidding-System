package com.auction.ui.dialogs;

import com.auction.database.BidderDAO; // Thay đổi từ UserDAO sang BidderDAO
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

    // SỬA: Sử dụng BidderDAO để xử lý nạp/rút
    private BidderDAO bidderDAO = new BidderDAO();
    private String currentUsername;
    private String mode = "DEPOSIT";

    public void setUsername(String username) {
        this.currentUsername = username;
    }

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
                // SỬA: bidderDAO vẫn gọi được getUserBalance vì nó kế thừa từ UserDAO
                double balance = bidderDAO.getUserBalance(currentUsername);
                if (amount > balance) {
                    lblStatus.setText("Số dư không đủ để rút!");
                    return;
                }
                // SỬA: Gọi hàm từ bidderDAO
                success = bidderDAO.requestWithdraw(currentUsername, amount);
            } else {
                // SỬA: Gọi hàm từ bidderDAO
                success = bidderDAO.requestDeposit(currentUsername, amount);
            }

            if (success) {
                // In ra console để debug (Long có thể xóa sau)
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
        if (txtAmount != null && txtAmount.getScene() != null) {
            Stage stage = (Stage) txtAmount.getScene().getWindow();
            stage.close();
        }
    }
}
module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // --- CẤP QUYỀN TRUY CẬP DỮ LIỆU (Dùng cho TableView / PropertyValueFactory) ---
    // Mở các package chứa Model để JavaFX lấy được data hiển thị lên bảng
    opens com.auction.common.item to javafx.base;
    opens com.auction.common.user to javafx.base;
    opens com.auction.transaction to javafx.base;

    // --- CẤP QUYỀN CHO FXML LOADER (Dùng cho giao diện) ---
    // Cho phép javafx.fxml truy cập vào các controller để ánh xạ fx:id và onAction
    opens com.auction.ui.auth to javafx.fxml;
    opens com.auction.ui.dashboard to javafx.fxml;
    opens com.auction.ui.tab to javafx.fxml;
    opens com.auction.ui.dialogs to javafx.fxml;

    // --- XUẤT PACKAGE (Dùng cho Runtime) ---
    exports com.auction.client;
    exports com.auction.ui.auth;
    exports com.auction.ui.dashboard;
    exports com.auction.ui.tab;
    exports com.auction.ui.dialogs;

    // Xuất util nếu NavigationService cần được gọi từ các module ngoài (thường là có)
    exports com.auction.util;
}
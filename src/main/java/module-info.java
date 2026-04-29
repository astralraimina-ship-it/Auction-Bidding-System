module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // 1. Mở package chứa các class Item (đã có)
    opens com.auction.common.item to javafx.base;

    // 2. THÊM DÒNG NÀY: Mở package chứa class User, Admin, Bidder...
    // Đây là chìa khóa để hết lỗi IllegalAccessException và hiện data lên bảng
    opens com.auction.common.user to javafx.base;

    // 3. Cho phép JavaFX load các file FXML từ package ui
    opens com.auction.ui to javafx.fxml;

    // Nếu bạn có dùng PropertyValueFactory cho các class trong database (ít khi dùng nhưng nên phòng hờ)
    // opens com.auction.database to javafx.base;

    exports com.auction.client;
    exports com.auction.ui;
}
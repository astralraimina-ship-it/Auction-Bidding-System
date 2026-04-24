module auction.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    // Phải mở và xuất khẩu tất cả các package con bạn đã tạo
    opens com.auction.client to javafx.fxml;
    opens com.auction.ui to javafx.fxml;

    exports com.auction.client;
    exports com.auction.common.auction;
    exports com.auction.common.item;
    exports com.auction.common.user;
    exports com.auction.common.exception;
}
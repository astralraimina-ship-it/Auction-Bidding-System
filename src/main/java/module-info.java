module auction.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.auction.client to javafx.fxml;

    exports com.auction.client;
    exports com.auction.common; // Sửa lại chữ c viết thường nếu đã rename folder
}
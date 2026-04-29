module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // QUAN TRỌNG: Mở package chứa các class Item (Art, Electronics,...)
    // để TableView có thể đọc dữ liệu qua Getter
    opens com.auction.common.item to javafx.base;

    // Cho phép JavaFX load các file FXML từ package ui
    opens com.auction.ui to javafx.fxml;

    exports com.auction.client;
    exports com.auction.ui;
}
module auction.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.auction.client to javafx.fxml;

    exports com.auction.client;
    exports com.auction.common;
    exports com.auction.common.item;
    exports com.auction.common.create;
    exports com.auction.common.user;
    exports com.auction.common.auction;
    exports com.auction.common.entity;
}
module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // --- CẤP QUYỀN TRUY CẬP DỮ LIỆU ---
    opens com.auction.common.item to javafx.base;
    opens com.auction.common.user to javafx.base;
    opens com.auction.transaction to javafx.base;

    // --- CẤP QUYỀN CHO FXML LOADER ---
    // Phải mở package chứa BidderAuctionRoomController (com.auction.gui)
    opens com.auction.ui.auth to javafx.fxml;
    opens com.auction.ui.dashboard to javafx.fxml;
    opens com.auction.ui.tab to javafx.fxml;
    opens com.auction.ui.dialogs to javafx.fxml;

    // --- XUẤT PACKAGE ---
    exports com.auction.client;
    exports com.auction.ui.auth;
    exports com.auction.ui.dashboard;
    exports com.auction.ui.tab;
    exports com.auction.ui.dialogs; // Xuất package mới
    exports com.auction.util;
}
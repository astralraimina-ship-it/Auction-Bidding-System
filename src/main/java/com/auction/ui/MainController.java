package com.auction.ui;

import com.auction.common.auction.Auction;
import com.auction.common.user.Seller;
import com.auction.ui.AuctionDetailController;
import javafx.collections.FXCollections; // Long thiếu cái này
import javafx.collections.ObservableList; // Và cái này
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalTime;

public class MainController {
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    // Danh sách dữ liệu
    private ObservableList<Auction> auctionData = FXCollections.observableArrayList();

//    @FXML
//    public void initialize() {
//        // 1. Kết nối các cột với dữ liệu (giữ nguyên)
//        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
//        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
//        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
//
//        // 2. Khóa tính năng Sort của từng cột
//        colItem.setSortable(false);
//        colPrice.setSortable(false);
//        colStatus.setSortable(false);
//
//        // 3. KHÓA TRIỆT ĐỂ: Vô hiệu hóa bộ lọc sắp xếp của bảng
//        // Điều này ngăn việc bảng tự động sắp xếp khi dữ liệu bên trong thay đổi
//        auctionTable.sortPolicyProperty().set(t -> null);
//
//        // 4. (Tùy chọn) Ngăn việc kéo thả để đổi thứ tự cột (Reordering)
//        // Giúp giao diện cố định hoàn toàn
//        auctionTable.widthProperty().addListener((observable, oldValue, newValue) -> {
//            TableHeaderRow header = (TableHeaderRow) auctionTable.lookup("TableHeaderRow");
//            if (header != null) {
//                header.reorderingProperty().addListener((obs, oldVal, newVal) -> header.setReordering(false));
//            }
//        });
//
//        // 5. Đổ dữ liệu vào bảng
//        auctionTable.setItems(auctionData);
//    }
@FXML
public void initialize() {
    // 1. Kết nối các cột với các thuộc tính trong class Auction
    // Đảm bảo trong class Auction có các hàm: getItemName(), getCurrentPrice(), getStatus()
    colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

    // 2. TẠO DỮ LIỆU TEST (Mock Data)
    // Giả sử bạn có class Item và Auction như trong cấu trúc project của bạn
    try {
        // Tạo một vật phẩm giả
        com.auction.common.item.Item testItem = new com.auction.common.item.Electronics("Laptop Dell XPS 15", "Máy tính cao cấp", "a");
        Seller seller = new Seller("a", "1");
        // Tạo một phiên đấu giá giả với giá khởi điểm 1500.0
        Auction testAuction = new Auction(seller, testItem, 1500.0, 2000, LocalTime.parse("12:50:00"));

        // Thêm vào danh sách quan sát (ObservableList)
        auctionData.add(testAuction);

        // Đổ danh sách vào bảng
        auctionTable.setItems(auctionData);

        System.out.println("✅ Đã chèn dữ liệu test thành công!");
    } catch (Exception e) {
        System.err.println("❌ Lỗi khi tạo dữ liệu test: " + e.getMessage());
        e.printStackTrace();
    }

    // 3. Khóa sắp xếp (như bạn yêu cầu lúc trước)
    colItem.setSortable(false);
    colPrice.setSortable(false);
    colStatus.setSortable(false);
    auctionTable.sortPolicyProperty().set(t -> null);
}

    @FXML
    public void handleJoin() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();

        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/ui/AuctionDetail.fxml"));
                Parent root = loader.load();

                AuctionDetailController detailController = loader.getController();

                // LƯU Ý: Nếu bên AuctionDetailController vẫn báo đỏ chỗ này,
                // Long hãy sang file đó sửa hàm setAuctionData chỉ nhận 1 tham số thôi nhé!
                detailController.setAuctionData(selected);

                Stage stage = new Stage();
                stage.setTitle("Phòng đấu giá: " + selected.getItem().getName());
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();

            } catch (IOException e) {
                System.err.println("❌ Lỗi đường dẫn FXML!");
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một vật phẩm!");
            alert.showAndWait();
        }
    }
}
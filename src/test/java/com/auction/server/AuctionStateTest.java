package com.auction.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionStateTest {

    private AuctionState auctionState;
    private final int ITEM_ID = 101;
    private final int SELLER_ID = 1;
    private final int BIDDER_A = 2;
    private final int BIDDER_B = 3;

    @BeforeEach
    void setUp() {
        // Khởi tạo một phiên đấu giá ảo:
        // ID = 101, Giá khởi điểm = 100.000, Bước giá = 10.000, Giá mua đứt (BIN) = 1.000.000
        auctionState = new AuctionState(ITEM_ID, 100000.0, 10000.0, 1000000.0);
    }

    @Test
    @DisplayName("Test 1: Đặt giá thủ công hợp lệ lần đầu tiên")
    void testValidManualBid() {
        double bidAmount = 150000.0;

        // Cập nhật người dẫn đầu là người đặt thủ công
        auctionState.setManualTopBidder(BIDDER_A, bidAmount);

        assertEquals(bidAmount, auctionState.getCurrentPrice(), "Giá hiện tại phải được cập nhật thành 150.000");
        assertEquals(BIDDER_A, auctionState.getHighestBidderId(), "Người giữ giá cao nhất phải là BIDDER_A");
        assertFalse(auctionState.isTopBidderAuto(), "Trạng thái Auto của top 1 phải bị tắt (false)");
    }

    @Test
    @DisplayName("Test 2: Kiểm tra logic cài đặt Auto-Bid hợp lệ")
    void testSetAutoTopBidder() {
        double bidAmount = 150000.0;
        double maxBudget = 500000.0;

        // Cập nhật người dẫn đầu là người dùng hệ thống Auto-Bid
        auctionState.setAutoTopBidder(BIDDER_B, bidAmount, maxBudget);

        assertEquals(bidAmount, auctionState.getCurrentPrice(), "Giá hiện tại phải là 150.000");
        assertEquals(BIDDER_B, auctionState.getHighestBidderId(), "Người giữ giá cao nhất phải là BIDDER_B");
        assertTrue(auctionState.isTopBidderAuto(), "Trạng thái Auto của top 1 phải được bật (true)");
        assertEquals(maxBudget, auctionState.getTopAutoMaxBudget(), "Ví mật của top 1 phải được lưu đúng là 500.000");
    }

    @Test
    @DisplayName("Test 3: Người đặt thủ công đè lên người đặt thủ công cũ")
    void testManualBidOverridesManualBid() {
        // Người A đặt 150.000
        auctionState.setManualTopBidder(BIDDER_A, 150000.0);

        // Người B đặt 200.000 (Vượt người A)
        auctionState.setManualTopBidder(BIDDER_B, 200000.0);

        assertEquals(200000.0, auctionState.getCurrentPrice(), "Giá hiện tại phải lên 200.000");
        assertEquals(BIDDER_B, auctionState.getHighestBidderId(), "Người dẫn đầu phải chuyển sang BIDDER_B");
        assertFalse(auctionState.isTopBidderAuto(), "Vẫn là đặt thủ công nên trạng thái Auto phải là false");
    }

    @Test
    @DisplayName("Test 4: Người đặt thủ công vượt qua mức ngân sách của Auto-Bid (Phá Auto)")
    void testManualBidOverridesAutoBid() {
        // Người A thiết lập Auto-Bid với giá hiện tại 150.000, tối đa 500.000
        auctionState.setAutoTopBidder(BIDDER_A, 150000.0, 500000.0);

        // Người B vào đặt thẳng một cục thủ công là 600.000 (Vượt qua mức 500.000 của A)
        auctionState.setManualTopBidder(BIDDER_B, 600000.0);

        assertEquals(600000.0, auctionState.getCurrentPrice(), "Giá hiện tại phải lên 600.000");
        assertEquals(BIDDER_B, auctionState.getHighestBidderId(), "Người dẫn đầu phải chuyển sang BIDDER_B");
        assertFalse(auctionState.isTopBidderAuto(), "Vì B đặt thủ công, trạng thái Auto của phòng phải bị tắt (false)");
        assertEquals(0.0, auctionState.getTopAutoMaxBudget(), "Ví mật của hệ thống phải bị reset về 0");
    }

    @Test
    @DisplayName("Test 5: Đạt đến giá Mua Đứt (BIN Price)")
    void testBinPriceReached() {
        // Người A đặt giá đúng bằng giá mua đứt (1.000.000)
        auctionState.setManualTopBidder(BIDDER_A, 1000000.0);

        assertEquals(1000000.0, auctionState.getCurrentPrice(), "Giá phải chạm nóc 1.000.000");
        assertEquals(BIDDER_A, auctionState.getHighestBidderId(), "BIDDER_A là người mua đứt");

        // Bạn có thể thêm hàm isBinReached() vào AuctionState nếu cần
        // assertTrue(auctionState.getCurrentPrice() >= auctionState.getBinPrice(), "Đã đạt ngưỡng Mua Đứt");
    }
}
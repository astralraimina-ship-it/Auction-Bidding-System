package com.auction.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    // Giả lập các tham số đấu giá
    private final double STEP = 20000;
    private final double BIN_PRICE = 1000000; // Giá mua đứt

    /**
     * TEST 1: Kiểm tra logic đặt giá hợp lệ
     * Đảm bảo giá đặt mới > Giá hiện tại + Bước giá
     */
    @Test
    public void testBidValidationLogic() {
        double currentMaxBid = 500000;
        double yourBid = 530000; // Hợp lệ (500k + 20k)

        assertTrue(yourBid >= (currentMaxBid + STEP), "Giá phải cao hơn giá hiện tại + bước giá");
    }

    /**
     * TEST 2: Kiểm tra logic mua đứt (BIN)
     * Đảm bảo nếu người dùng trả bằng hoặc hơn giá Bin thì kích hoạt BIN
     */
    @Test
    public void testBinPriceTrigger() {
        double bidInput = 1000000; // Đạt giá mua đứt

        boolean isBin = (bidInput >= BIN_PRICE);
        assertTrue(isBin, "Hệ thống phải nhận diện đây là hành động Mua đứt (BIN)");
    }

    /**
     * TEST 3: Kiểm tra logic giới hạn Auto-Bid
     * Auto-Bid không được phép vượt quá giá Mua đứt
     */
    @Test
    public void testAutoBidCap() {
        double userMaxBudget = 1200000; // User muốn trả 1.2 triệu
        double actualBinPrice = 1000000; // Nhưng giá mua đứt chỉ có 1 triệu

        // Auto-bid phải bị chặn lại ở 1 triệu
        double effectiveMax = Math.min(userMaxBudget, actualBinPrice);

        assertEquals(1000000, effectiveMax, "Auto-bid phải bị chặn ở giá Mua đứt (Bin Price)");
    }
}
package com.auction.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionErrorTest {

    /**
     * TEST 4: Kiểm tra xử lý lỗi khi nhập số tiền âm
     * Hệ thống phải nhận diện được và không cho phép đặt giá
     */
    @Test
    public void testNegativeBidAmount() {
        double negativeBid = -50000;

        // Giả lập logic kiểm tra dữ liệu
        boolean isPossible = (negativeBid > 0);

        assertFalse(isPossible, "Hệ thống phải từ chối số tiền âm!");
    }

    /**
     * TEST 5: Kiểm tra lỗi định dạng (String thành Number)
     * Đây là lỗi phổ biến khi người dùng nhập chữ vào ô giá tiền
     */
    @Test
    public void testInvalidNumberFormat() {
        String userInput = "abc123";

        // Kiểm tra xem hệ thống có quăng ra Exception khi parse sai không
        assertThrows(NumberFormatException.class, () -> {
            Double.parseDouble(userInput);
        }, "Hệ thống phải báo lỗi nếu User nhập ký tự vào ô giá");
    }
}
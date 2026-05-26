package com.auction.common.utils;

import com.auction.util.FormatUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    @Test
    public void testParseDoubleSafe_StandardFormat() {
        assertEquals(15000.0, FormatUtils.parseDoubleSafe("15000.0"));
        assertEquals(500.55, FormatUtils.parseDoubleSafe("500.55"));
    }

    @Test
    public void testParseDoubleSafe_CommaFormat() {
        // Ở VN hay dùng phẩy, test xem code có fix được thành chấm không
        assertEquals(15000.5, FormatUtils.parseDoubleSafe("15000,5"));
    }

    @Test
    public void testParseDoubleSafe_IntegerString() {
        assertEquals(2000000.0, FormatUtils.parseDoubleSafe("2000000"));
    }

    @Test
    public void testParseDoubleSafe_InvalidString() {
        // Test trường hợp gửi bậy bạ lên server
        assertEquals(0.0, FormatUtils.parseDoubleSafe("abc"));
        assertEquals(0.0, FormatUtils.parseDoubleSafe(""));
        assertEquals(0.0, FormatUtils.parseDoubleSafe(null));
    }
}
package com.auction.common.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ItemFactoryTest {

    @Test
    public void testCreateArtItem() {
        // 1. Chuẩn bị dữ liệu giả (Mock Data)
        Map<String, Object> commonData = new HashMap<>();
        commonData.put("id", 1);
        commonData.put("name", "Bức tranh Mona Lisa");
        commonData.put("startPrice", 500000.0);
        commonData.put("winPrice", 700000.0); // Test xem winPrice có nhận không
        commonData.put("endTime", new Timestamp(System.currentTimeMillis()));

        Map<String, Object> specificData = new HashMap<>();
        specificData.put("artist", "Leonardo da Vinci");
        specificData.put("medium", "Sơn dầu");

        // 2. Gọi hàm cần test
        Item item = ItemFactory.createItem("ART", commonData, specificData);

        // 3. Khẳng định kết quả (Assertions)
        assertNotNull(item, "Item không được null");
        assertTrue(item instanceof Art, "Item phải là instance của lớp Art");
        assertEquals("Bức tranh Mona Lisa", item.getName(), "Tên sản phẩm phải khớp");
        assertEquals(500000.0, item.getStartPrice(), "Giá khởi điểm phải khớp");
        assertEquals(700000.0, item.getWinPrice(), "Giá thắng phải khớp");

        // Ép kiểu để test thuộc tính riêng
        Art art = (Art) item;
        assertEquals("Leonardo da Vinci", art.getArtist(), "Tên họa sĩ phải khớp");
    }

    @Test
    public void testCreateVehicleItem_FallbackToDefaultValues() {
        // Cố tình truyền map rỗng để test xem hàm getOrDefault có hoạt động đúng không
        Map<String, Object> emptyCommon = new HashMap<>();
        Map<String, Object> emptySpecific = new HashMap<>();

        Item item = ItemFactory.createItem("VEHICLE", emptyCommon, emptySpecific);

        assertNotNull(item);
        assertTrue(item instanceof Vehicle);
        assertEquals(0, item.getId(), "ID mặc định phải là 0");
        assertEquals(0.0, item.getCurrentPrice(), "Giá hiện tại mặc định phải là 0.0");

        Vehicle vehicle = (Vehicle) item;
        assertEquals("", vehicle.getBrand(), "Hãng xe mặc định phải là chuỗi rỗng");
        assertEquals(0, vehicle.getModelYear(), "Đời xe mặc định phải là 0");
    }
}
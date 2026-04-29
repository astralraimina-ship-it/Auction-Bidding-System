package com.auction.common.item;

/**
 * Lớp đại diện cho các sản phẩm không thuộc danh mục đặc thù.
 * Kế thừa toàn bộ hệ thống Getter/Setter từ lớp cha Item.
 */
public class Other extends Item {
    private static final long serialVersionUID = 1L;

    public Other(int id, String name, String description, double startPrice, double binPrice, double step) {
        // Mặc định category là "OTHER"
        super(id, name, description, startPrice, binPrice, step, "OTHER");
    }

    /**
     * Mặc dù Other hiện tại chỉ dùng các thuộc tính của lớp cha Item,
     * nhưng nó vẫn kế thừa toàn bộ Getter/Setter từ Item:
     * - getName() / setName()
     * - getStartPrice() / setStartPrice()
     * - ...
     */

    @Override
    public String getItemDetails() {
        return String.format("Sản phẩm khác: %s | Giá khởi điểm: %.2f", getName(), getStartPrice());
    }
}
package com.auction.common.item;

import java.sql.Timestamp;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private String medium;
    private String state;

    // ĐÃ SỬA: Thêm String imagePath vào constructor và truyền lên super()
    public Art(int id, String name, String description, double startPrice, double binPrice, double currentPrice, double step,
               String sellerName, Timestamp endTime, String status, String paymentStatus, String artist, String medium, String state, double winPrice, String imagePath) {
        super(id, name, description, startPrice, binPrice, currentPrice, winPrice, step, sellerName, "ART", endTime, status, paymentStatus, imagePath);
        this.artist = artist;
        this.medium = medium;
        this.state = state;
        String oldDescription = this.getDescription();
        this.setDescription(oldDescription + this.getItemDetails());
    }

    @Override
    public String getItemDetails() {
        return String.format("Họa sĩ: %s | Chất liệu: %s | Tình trạng: %s", artist, medium, state);
    }

    public String getArtist() { return artist; }
    public String getMedium() { return medium; }
    public String getState() { return state; }
}
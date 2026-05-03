package com.auction.common.item;

import java.sql.Timestamp;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private String medium;
    private String state;

    public Art(int id, String name, String description, double startPrice, double binPrice, double step,
               String sellerName, Timestamp endTime, String status, String artist, String medium, String state) {
        super(id, name, description, startPrice, binPrice, step, sellerName, "ART", endTime, status);
        this.artist = artist;
        this.medium = medium;
        this.state = state;
    }

    @Override
    public String getItemDetails() {
        return String.format("Họa sĩ: %s | Chất liệu: %s | Tình trạng: %s", artist, medium, state);
    }

    public String getArtist() { return artist; }
    public String getMedium() { return medium; }
    public String getState() { return state; }
}
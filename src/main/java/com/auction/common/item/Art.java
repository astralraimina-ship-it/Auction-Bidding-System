package com.auction.common.item;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private String medium;

    public Art(int id, String name, String description, double startPrice, double binPrice, double step, String artist, String medium) {
        super(id, name, description, startPrice, binPrice, step, "ART");
        this.artist = artist;
        this.medium = medium;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    @Override
    public String getItemDetails() { return "Tác phẩm nghệ thuật của " + artist; }
}
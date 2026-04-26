package com.example.auctionfx.model;

public class Art extends Item {
    private String artist;
    private Integer yearCreated;

    public Art() {}

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public Integer getYearCreated() { return yearCreated; }
    public void setYearCreated(Integer yearCreated) { this.yearCreated = yearCreated; }
}

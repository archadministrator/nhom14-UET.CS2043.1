package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ART")
public class Art extends Item {
    
    private String artist;
    private Integer yearCreated;

    public Art() {
        super();
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public Integer getYearCreated() { return yearCreated; }
    public void setYearCreated(Integer yearCreated) { this.yearCreated = yearCreated; }

    @Override
    public String getCategory() {
        return "ART";
    }
}

package com.example.auctionfx.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "item_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Electronics.class, name = "ELECTRONICS"),
    @JsonSubTypes.Type(value = Art.class, name = "ART"),
    @JsonSubTypes.Type(value = Vehicle.class, name = "VEHICLE")
})
public class Item {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;

    public Item() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

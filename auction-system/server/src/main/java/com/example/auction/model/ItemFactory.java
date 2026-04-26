package com.example.auction.model;

public class ItemFactory {
    public static Item createItem(String type, String name, String description) {
        Item item;
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                item = new Electronics();
                break;
            case "ART":
                item = new Art();
                break;
            case "VEHICLE":
                item = new Vehicle();
                break;
            default:
                throw new IllegalArgumentException("Unknown item type: " + type);
        }
        item.setName(name);
        item.setDescription(description);
        return item;
    }
}

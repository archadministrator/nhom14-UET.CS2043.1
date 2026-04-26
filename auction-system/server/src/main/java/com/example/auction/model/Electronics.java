package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ELECTRONICS")
public class Electronics extends Item {
    
    private String brand;
    private String model;

    public Electronics() {
        super();
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    @Override
    public String getCategory() {
        return "ELECTRONICS";
    }
}

package com.example.auctionfx.model;

public class Electronics extends Item {
    private String brand;
    private String model;

    public Electronics() {}

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}

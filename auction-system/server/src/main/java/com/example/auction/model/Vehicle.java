package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VEHICLE")
public class Vehicle extends Item {
    
    private String make;
    private String vehicleModel;
    private Integer year;

    public Vehicle() {
        super();
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    @Override
    public String getCategory() {
        return "VEHICLE";
    }
}

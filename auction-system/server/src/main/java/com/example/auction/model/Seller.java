package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SELLER")
public class Seller extends User {
    
    public Seller() {
        super();
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}

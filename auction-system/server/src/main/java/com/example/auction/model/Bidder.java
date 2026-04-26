package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BIDDER")
public class Bidder extends User {
    
    public Bidder() {
        super();
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}

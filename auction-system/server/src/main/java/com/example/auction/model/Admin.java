package com.example.auction.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    
    public Admin() {
        super();
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}

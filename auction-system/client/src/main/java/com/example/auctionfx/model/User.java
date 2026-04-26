package com.example.auctionfx.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "roleName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Bidder.class, name = "BIDDER"),
    @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
    @JsonSubTypes.Type(value = Admin.class, name = "ADMIN")
})
public abstract class User {
    private Long id;
    private String username;
    private String email;
    private Double balance;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public abstract String getRoleName();
}
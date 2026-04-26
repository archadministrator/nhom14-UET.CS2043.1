package com.example.auction.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "roleName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Bidder.class, name = "BIDDER"),
    @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
    @JsonSubTypes.Type(value = Admin.class, name = "ADMIN")
})
public abstract class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    private Double balance = 0.0;

    public User() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public abstract String getRoleName();
}
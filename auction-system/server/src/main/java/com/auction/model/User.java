package com.auction.model;


import com.auction.model.enums.Role;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User {
    private long id;
    private String username;
    private String password;
    private String email;
    private Role role;
    private BigDecimal balance;
    private final LocalDateTime createdAt;
    private boolean isActive;

    public User(){
        this.balance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public User(long id, String username, String email,String password, Role role){
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.balance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    //Getter, Setter
    public long getId(){return id;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public String getEmail(){return email;}
    public Role getRole(){return role;}
    public BigDecimal getBalance(){return balance;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public boolean isActive(){return isActive;}
    
    public void setId(long id){this.id = id;}
    public void setUsername(String username){this.username = username;}
    public void setEmail(String email){this.email = email;}
    public void setPassword(String password){this.password = password;}
    public void setRole(Role role){this.role = role;}
    public void setBalance(BigDecimal balance){this.balance = balance;}

    public void deposit(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO)>0){
            this.balance = this.balance.add(amount);
        }
    }

    public boolean withdraw(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO)>0){
            if (this.balance.compareTo(amount)<0){
                return false;
            } 
            this.balance = this.balance.subtract(amount);
            return true;
        } 
        return false;
    }

}
package com.example.auction.dto;

public class AuthResponse {
    private String jwt;
    private String username;
    private String role;

    public AuthResponse(String jwt, String username, String role) {
        this.jwt = jwt;
        this.username = username;
        this.role = role;
    }

    public String getJwt() { return jwt; }
    public void setJwt(String jwt) { this.jwt = jwt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

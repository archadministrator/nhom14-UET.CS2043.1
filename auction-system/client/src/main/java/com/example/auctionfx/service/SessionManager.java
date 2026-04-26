package com.example.auctionfx.service;

import com.example.auctionfx.model.User;

/**
 * Singleton Pattern to manage user session.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String jwtToken;
    private String role;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void logout() {
        this.currentUser = null;
        this.jwtToken = null;
        this.role = null;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}

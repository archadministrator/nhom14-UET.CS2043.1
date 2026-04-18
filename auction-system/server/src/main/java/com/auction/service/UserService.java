package com.auction.service;

import com.auction.model.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private List<User> users = new ArrayList<>();

    public User register(String username, String email, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                System.out.println("Username already exists");
                return null;
            }
        }

        User newUser = new User(null, username, email, password, null);
        users.add(newUser);
        System.out.println("Register Successfully: " + newUser.getUsername());
        return newUser;
    }

    public User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                System.out.println("Login Successfully: " + user.getUsername());
                return user;
            }
        }

        System.out.println("Wrong username or password");
        return null;
    }

    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        System.out.println("Can't find username: " + username);
        return null;
    }
}
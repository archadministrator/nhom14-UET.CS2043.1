package com.example.auction.service;

import com.example.auction.model.*;
import com.example.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void initializeData() {
        try {
            System.out.println(">>> [SERVER] Đang khởi tạo dữ liệu mặc định...");
            
            // Khởi tạo các tài khoản chính
            initUser("admin", "admin123", "admin@auction.com", "ADMIN");
            initUser("seller", "seller123", "seller@auction.com", "SELLER");
            initUser("bidder", "bidder123", "bidder@auction.com", "BIDDER");
            
            // Khởi tạo thêm tài khoản test mới theo yêu cầu
            initUser("user1", "user123", "user1@auction.com", "BIDDER");
            initUser("user2", "user223", "user2@auction.com", "BIDDER");
            initUser("user3", "user323", "user3@auction.com", "SELLER");

            System.out.println(">>> [SUCCESS] Server đã sẵn sàng. Bạn có thể đăng nhập ngay bây giờ.");
        } catch (Exception e) {
            System.err.println(">>> [CRITICAL ERROR] Không thể khởi tạo dữ liệu: " + e.getMessage());
        }
    }

    private void initUser(String username, String password, String email, String role) {
        try {
            Optional<User> existing = userRepository.findByUsername(username);
            User user;
            if (existing.isPresent()) {
                user = existing.get();
                user.setPassword(passwordEncoder.encode(password));
            } else {
                if ("ADMIN".equalsIgnoreCase(role)) {
                    user = new Admin();
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    user = new Seller();
                } else {
                    user = new Bidder();
                    user.setBalance(10000.0);
                }
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(password));
                user.setEmail(email);
            }
            userRepository.saveAndFlush(user);
            System.out.println(">>> OK: " + username + " (" + role + ")");
        } catch (Exception e) {
            System.err.println(">>> Lỗi khi tạo user " + username + ": " + e.getMessage());
        }
    }

    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User register(String username, String password, String email, String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Tên người dùng đã tồn tại!");
        }

        User user;
        if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller();
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin();
        } else {
            user = new Bidder();
            user.setBalance(0.0);
        }

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        
        return userRepository.saveAndFlush(user);
    }
}
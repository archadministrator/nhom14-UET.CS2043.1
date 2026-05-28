package com.auction.service;

import com.auction.dao.UserRepository;
import com.auction.exception.UserAlreadyExistsException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.User;
import com.auction.util.Dto;
import com.auction.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final SimpMessagingTemplate messagingTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager, JwtUtil jwtUtil,
                       SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Dto.AuthResponse register(Dto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new UserAlreadyExistsException("Username '" + req.username() + "' đã tồn tại.");
        if (userRepository.existsByEmail(req.email()))
            throw new UserAlreadyExistsException("Email '" + req.email() + "' đã được đăng ký.");

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .balance(BigDecimal.ZERO)
                .build();

        userRepository.save(user);
        log.info("User đăng ký: {} ({})", user.getUsername(), user.getRole());

        String token = jwtUtil.generateToken(user);
        return new Dto.AuthResponse(token, user.getUsername(), user.getEmail(),
                user.getRole(), user.getBalance());
    }

    public Dto.AuthResponse login(Dto.LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        User user = findByUsername(req.username());
        String token = jwtUtil.generateToken(user);
        log.info("User đăng nhập: {}", user.getUsername());
        return new Dto.AuthResponse(token, user.getUsername(), user.getEmail(),
                user.getRole(), user.getBalance());
    }

    @Transactional(readOnly = true)
    public Dto.UserResponse getProfile(String username) {
        return Dto.UserResponse.from(findByUsername(username));
    }

    @Transactional
    public Dto.UserResponse topUpBalance(String username, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        User user = findByUsername(username);
        user.setBalance(user.getBalance().add(amount));
        return Dto.UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void subtractBalance(String username, BigDecimal amount) {
        User user = findByUsername(username);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư không đủ.");
        }
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
    }

    @Transactional
    public void addBalance(String username, BigDecimal amount) {
        User user = findByUsername(username);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Dto.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> Dto.UserResponse.from(u))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user id: " + userId));
        user.setActive(active);
        userRepository.save(user);

        if (!active) {
            messagingTemplate.convertAndSend("/topic/user/" + user.getUsername(),
                    new Dto.AccountStatusMessage("ACCOUNT_LOCKED", user.getUsername(), "Tài khoản của bạn đã bị khóa bởi Quản trị viên."));
            log.info("Đã gửi broadcast khóa tài khoản cho user: {}", user.getUsername());
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user: " + username));
    }
}
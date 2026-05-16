package com.auction.service;

import com.auction.dao.UserRepository;
import com.auction.exception.UserAlreadyExistsException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.User;
import com.auction.model.enums.Role;
import com.auction.util.Dto;
import com.auction.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtUtil jwtUtil;

    @InjectMocks UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("bidder01")
                .email("bidder01@test.com")
                .password("encoded_pass")
                .role(Role.BIDDER)
                .balance(new BigDecimal("500000"))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Đăng ký thành công → trả về AuthResponse có token")
        void register_success() {
            Dto.RegisterRequest req = new Dto.RegisterRequest(
                    "newuser", "new@test.com", "password123", Role.BIDDER);

            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("hashed");
            given(userRepository.save(any(User.class))).willReturn(sampleUser);
            given(jwtUtil.generateToken(any(User.class))).willReturn("jwt-token");

            Dto.AuthResponse result = userService.register(req);

            assertThat(result.token()).isEqualTo("jwt-token");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("Username đã tồn tại → ném UserAlreadyExistsException")
        void register_duplicateUsername_throws() {
            Dto.RegisterRequest req = new Dto.RegisterRequest(
                    "bidder01", "other@test.com", "pass", Role.BIDDER);
            given(userRepository.existsByUsername("bidder01")).willReturn(true);

            assertThatThrownBy(() -> userService.register(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("bidder01");
        }

        @Test
        @DisplayName("Email đã tồn tại → ném UserAlreadyExistsException")
        void register_duplicateEmail_throws() {
            Dto.RegisterRequest req = new Dto.RegisterRequest(
                    "newuser2", "bidder01@test.com", "pass", Role.BIDDER);
            given(userRepository.existsByUsername("newuser2")).willReturn(false);
            given(userRepository.existsByEmail("bidder01@test.com")).willReturn(true);

            assertThatThrownBy(() -> userService.register(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("bidder01@test.com");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Đăng nhập thành công → trả về token")
        void login_success() {
            Dto.LoginRequest req = new Dto.LoginRequest("bidder01", "password123");
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));
            given(jwtUtil.generateToken(sampleUser)).willReturn("jwt-token");

            Dto.AuthResponse result = userService.login(req);

            assertThat(result.token()).isEqualTo("jwt-token");
            assertThat(result.username()).isEqualTo("bidder01");
            then(authManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Username không tồn tại → ném UserNotFoundException")
        void login_userNotFound_throws() {
            Dto.LoginRequest req = new Dto.LoginRequest("ghost", "pass");
            given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(req))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TOP UP BALANCE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("topUpBalance()")
    class TopUpBalance {

        @Test
        @DisplayName("Nạp tiền hợp lệ → số dư tăng đúng")
        void topUp_valid_balanceIncreased() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(User.class))).willReturn(sampleUser);

            userService.topUpBalance("bidder01", new BigDecimal("100000"));

            assertThat(sampleUser.getBalance()).isEqualByComparingTo("600000");
        }

        @Test
        @DisplayName("Nạp số tiền = 0 → ném IllegalArgumentException")
        void topUp_zeroAmount_throws() {
            assertThatThrownBy(() -> userService.topUpBalance("bidder01", BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lớn hơn 0");
        }

        @Test
        @DisplayName("Nạp số tiền âm → ném IllegalArgumentException")
        void topUp_negativeAmount_throws() {
            assertThatThrownBy(() -> userService.topUpBalance("bidder01", new BigDecimal("-1000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SUBTRACT BALANCE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("subtractBalance()")
    class SubtractBalance {

        @Test
        @DisplayName("Số dư đủ → trừ thành công")
        void subtract_sufficient_balance() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(User.class))).willReturn(sampleUser);

            userService.subtractBalance("bidder01", new BigDecimal("200000"));

            assertThat(sampleUser.getBalance()).isEqualByComparingTo("300000");
        }

        @Test
        @DisplayName("Số dư không đủ → ném RuntimeException")
        void subtract_insufficient_throws() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> userService.subtractBalance("bidder01", new BigDecimal("999999")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Số dư không đủ");
        }

        @Test
        @DisplayName("Số dư đúng bằng → trừ thành công, còn 0")
        void subtract_exactBalance_resultZero() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(User.class))).willReturn(sampleUser);

            userService.subtractBalance("bidder01", new BigDecimal("500000"));

            assertThat(sampleUser.getBalance()).isEqualByComparingTo("0");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADD BALANCE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addBalance()")
    class AddBalance {

        @Test
        @DisplayName("Cộng tiền → số dư tăng đúng")
        void addBalance_success() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(User.class))).willReturn(sampleUser);

            userService.addBalance("bidder01", new BigDecimal("50000"));

            assertThat(sampleUser.getBalance()).isEqualByComparingTo("550000");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET PROFILE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("Lấy profile thành công → trả về đúng thông tin")
        void getProfile_success() {
            given(userRepository.findByUsername("bidder01")).willReturn(Optional.of(sampleUser));

            Dto.UserResponse result = userService.getProfile("bidder01");

            assertThat(result.username()).isEqualTo("bidder01");
            assertThat(result.email()).isEqualTo("bidder01@test.com");
            assertThat(result.role()).isEqualTo(Role.BIDDER);
        }

        @Test
        @DisplayName("Không tìm thấy user → ném UserNotFoundException")
        void getProfile_notFound_throws() {
            given(userRepository.findByUsername("nobody")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile("nobody"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: GET ALL / SET ACTIVE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Admin operations")
    class AdminOps {

        @Test
        @DisplayName("getAllUsers() → trả về danh sách đầy đủ")
        void getAllUsers_returnsAll() {
            User user2 = User.builder().id(2L).username("seller01")
                    .email("s@test.com").role(Role.SELLER)
                    .balance(BigDecimal.ZERO).build();
            given(userRepository.findAll()).willReturn(List.of(sampleUser, user2));

            List<Dto.UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Dto.UserResponse::username)
                    .containsExactlyInAnyOrder("bidder01", "seller01");
        }

        @Test
        @DisplayName("setActive(false) → user bị vô hiệu hóa")
        void setActive_false_disablesUser() {
            given(userRepository.findById(1L)).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(User.class))).willReturn(sampleUser);

            userService.setActive(1L, false);

            assertThat(sampleUser.isActive()).isFalse();
        }

        @Test
        @DisplayName("setActive với userId không tồn tại → ném UserNotFoundException")
        void setActive_notFound_throws() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.setActive(99L, true))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
package auction.server.service;

import auction.server.model.User;
import auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho UserService.
 * Dùng Mockito để mock UserRepository — không cần kết nối DB thật.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository mockUserRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Inject mock repository qua package-private constructor
        userService = new UserService(mockUserRepository);
    }

    // ─── register() ────────────────────────────────────────────

    @Test
    @DisplayName("Đăng ký thành công với thông tin hợp lệ")
    void register_validInput_returnsSuccess() {
        when(mockUserRepository.isUsernameExist("alice")).thenReturn(false);

        String result = userService.register("alice", "password123", "BIDDER");

        assertEquals("success", result);
        verify(mockUserRepository, times(1)).saveUser(any());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username để trống")
    void register_emptyUsername_returnsError() {
        String result = userService.register("", "password123", "BIDDER");

        assertEquals("Tài khoản hoặc mật khẩu không được để trống!", result);
        verify(mockUserRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi password để trống")
    void register_emptyPassword_returnsError() {
        String result = userService.register("alice", "", "BIDDER");

        assertEquals("Tài khoản hoặc mật khẩu không được để trống!", result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi password quá ngắn (< 3 ký tự)")
    void register_shortPassword_returnsError() {
        String result = userService.register("alice", "ab", "BIDDER");

        assertEquals("Mật khẩu phải có ít nhất 3 ký tự!", result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi role để trống")
    void register_emptyRole_returnsError() {
        String result = userService.register("alice", "password123", "");

        assertEquals("Vui lòng chọn quyền (BIDDER hoặc SELLER)!", result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username đã tồn tại")
    void register_duplicateUsername_returnsError() {
        when(mockUserRepository.isUsernameExist("alice")).thenReturn(true);

        String result = userService.register("alice", "password123", "BIDDER");

        assertEquals("Tài khoản này đã có người sử dụng!", result);
        verify(mockUserRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi role không hợp lệ")
    void register_invalidRole_returnsError() {
        when(mockUserRepository.isUsernameExist("alice")).thenReturn(false);

        String result = userService.register("alice", "password123", "UNKNOWN_ROLE");

        assertEquals("Quyền không hợp lệ!", result);
    }

    @Test
    @DisplayName("Đăng ký thành công với role SELLER (không phân biệt hoa thường)")
    void register_sellerRoleCaseInsensitive_returnsSuccess() {
        when(mockUserRepository.isUsernameExist("bob")).thenReturn(false);

        String result = userService.register("bob", "password123", "seller");

        assertEquals("success", result);
    }

    // ─── login() ───────────────────────────────────────────────

    @Test
    @DisplayName("Đăng nhập thành công với thông tin đúng")
    void login_correctCredentials_returnsSuccess() {
        when(mockUserRepository.checkLogin("alice", "password123")).thenReturn(true);

        String result = userService.login("alice", "password123");

        assertEquals("success", result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại với mật khẩu sai")
    void login_wrongPassword_returnsError() {
        when(mockUserRepository.checkLogin("alice", "wrongpass")).thenReturn(false);

        String result = userService.login("alice", "wrongpass");

        assertEquals("Sai tài khoản hoặc mật khẩu!", result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi username null")
    void login_nullUsername_returnsError() {
        String result = userService.login(null, "password123");

        assertEquals("Dữ liệu không hợp lệ!", result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi password null")
    void login_nullPassword_returnsError() {
        String result = userService.login("alice", null);

        assertEquals("Dữ liệu không hợp lệ!", result);
    }

    // ─── getUserByUsername() ────────────────────────────────────

    @Test
    @DisplayName("Tìm user theo username — trả về đúng user")
    void getUserByUsername_existingUser_returnsUser() {
        User mockUser = mock(User.class);
        when(mockUser.getUsername()).thenReturn("alice");
        when(mockUserRepository.getAllUsers()).thenReturn(List.of(mockUser));

        User result = userService.getUserByUsername("alice");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    @DisplayName("Tìm user theo username — trả về null nếu không tồn tại")
    void getUserByUsername_notFound_returnsNull() {
        when(mockUserRepository.getAllUsers()).thenReturn(List.of());

        User result = userService.getUserByUsername("ghost");

        assertNull(result);
    }
}

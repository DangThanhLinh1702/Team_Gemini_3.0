package auction.server.service;

import auction.server.model.Admin;
import auction.server.model.Bidder;
import auction.server.model.Seller;
import auction.server.model.User;
import auction.server.repository.UserRepository;
import java.util.List;

public class UserService implements IUserService {

    private final UserRepository userRepository;

    /** Constructor mặc định — dùng trong production. */
    public UserService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Constructor cho testing (Dependency Injection).
     * Cho phép truyền mock UserRepository vào, không cần kết nối DB thật.
     */
    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String register(String username, String password, String role) {

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return "Tài khoản hoặc mật khẩu không được để trống!";
        }

        if (password.length() < 3) {
            return "Mật khẩu phải có ít nhất 3 ký tự!";
        }

        if (role == null || role.trim().isEmpty()) {
            return "Vui lòng chọn quyền (BIDDER hoặc SELLER)!";
        }
        if (userRepository.isUsernameExist(username)) {
            return "Tài khoản này đã có người sử dụng!";
        }

        String upperRole = role.toUpperCase();
        // ADMIN không thể được tạo qua đăng ký thông thường
        if (upperRole.equals("ADMIN")) {
            return "Không được phép đăng ký tài khoản ADMIN!";
        }
        User newUser;
        if (upperRole.equals("BIDDER")) {
            newUser = new Bidder(username, password);
        } else if (upperRole.equals("SELLER")) {
            newUser = new Seller(username, password);
        } else {
            return "Quyền không hợp lệ! Chỉ chấp nhận BIDDER hoặc SELLER.";
        }

        userRepository.saveUser(newUser);
        return "success";
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return "Dữ liệu không hợp lệ!";
        }

        boolean isExist = userRepository.checkLogin(username, password);
        if (isExist) {
            return "success";
        } else {
            return "Sai tài khoản hoặc mật khẩu!";
        }
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public User loginAndGetUser(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        // Từ chối đăng nhập nếu tài khoản bị block
        if (userRepository.isUserBlocked(username)) {
            return null; // Trả null để AuthHandler trả lỗi 401
        }
        boolean isValid = userRepository.checkLogin(username, password);
        if (isValid) {
            return userRepository.getAllUsers().stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public boolean deleteUser(String username) {
        return userRepository.deleteUser(username);
    }

    public boolean blockUser(String username) {
        return userRepository.setUserBlocked(username, true);
    }

    public boolean unblockUser(String username) {
        return userRepository.setUserBlocked(username, false);
    }

    public boolean isUserBlocked(String username) {
        return userRepository.isUserBlocked(username);
    }

    public User getUserByUsername(String username) {
        return userRepository.getAllUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}

package auction.server.service;

import auction.server.model.Admin;
import auction.server.model.Bidder;
import auction.server.model.Seller;
import auction.server.model.User;
import auction.server.repository.UserRepository;
import java.util.List;

public class UserService {
    private final UserRepository userRepository = new UserRepository();

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
        User newUser;
        if (upperRole.equals("BIDDER")) {
            newUser = new Bidder(username, password);
        } else if (upperRole.equals("SELLER")) {
            newUser = new Seller(username, password);
        } else if (upperRole.equals("ADMIN")) {
            newUser = new Admin(username, password);
        } else {
            return "Quyền không hợp lệ!";
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
}
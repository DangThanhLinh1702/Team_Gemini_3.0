package auction.server.service;

import auction.server.repository.UserRepository;

import java.util.List;

public class UserService implements IUserService{
    private final UserRepository userRepository = new UserRepository();

    public String register(String username, String password) {

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return "Tài khoản hoặc mật khẩu không được để trống!";
        }
        // trim():xoa space , isEmpty():kiem tra chuoi rong

        if (password.length() < 3) {
            return "Mật khẩu phải có ít nhất 3 ký tự!";
        }

        if (userRepository.isUsernameExist(username)) {
            return "Tài khoản này đã có người sử dụng!";
        }

        userRepository.saveUser(username, password);
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
    public List<String[]> getAllUsers() {
        return userRepository.getAllUsers();
    }
}
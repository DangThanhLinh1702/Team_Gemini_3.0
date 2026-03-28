package auction.server.repository;

import auction.server.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final List<String[]> allUsers = new ArrayList<>();

    static {
        allUsers.add(new String[]{"admin", "123456"});
        allUsers.add(new String[]{"seller1", "matkhau1"});
    }

    public boolean isUsernameExist(String username) {
        for (String[] user : allUsers) {
            if (user[0].equals(username)) {
                return true;

            }
        }
        return false;
    }

    public void saveUser(String username, String password) {
        allUsers.add(new String[]{username, password});
        System.out.println("Database: Đã thêm user mới -> " + username);
    }

    public boolean checkLogin(String username, String password) {
        for (String[] user : allUsers) {
            if (user[0].equals(username) && user[1].equals(password)) {
                return true;
            }
        }
        return false;
    }

    public List<String[]> getAllUsers() {
        return allUsers;
    }
}
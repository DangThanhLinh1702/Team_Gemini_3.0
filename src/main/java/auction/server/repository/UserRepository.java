package auction.server.repository;

import auction.server.model.Admin;
import auction.server.model.Bidder;
import auction.server.model.Seller;
import auction.server.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
        private static final List<User> allUsers = new ArrayList<>();

        static {
            allUsers.add(new Admin("admin", "123456"));
            allUsers.add(new Seller("seller1", "matkhau1"));
            allUsers.add(new Bidder("bidder1", "031007"));
        }

        public boolean isUsernameExist(String username) {
            for (User user : allUsers) {
                if (user.getUsername().equals(username)) return true;
            }
            return false;
        }

        public void saveUser(User newUser) {
            allUsers.add(newUser);
            System.out.println("Đã thêm: " + newUser.getUsername() + " - Quyền: " + newUser.getRole());
        }

        public boolean checkLogin(String username, String password) {
            for (User user : allUsers) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) return true;
            }
            return false;
        }

        public List<User> getAllUsers() {
            return allUsers;
        }
        public User findById(int id) {
            for (User u : allUsers) {
                if (u.getId() == id) return u;
            }
            return null;
        }
}
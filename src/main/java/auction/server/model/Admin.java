package auction.server.model;

public class Admin extends User {
    public Admin(String username, String password) {
        super(username, password, "ADMIN");
    }
}
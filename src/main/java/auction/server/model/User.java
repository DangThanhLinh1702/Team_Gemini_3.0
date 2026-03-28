package auction.server.model;

public abstract class User extends Entity {
    private String username;
    private String password;

    // Constructor (Hàm khởi tạo)
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Các hàm Get/Set cơ bản
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

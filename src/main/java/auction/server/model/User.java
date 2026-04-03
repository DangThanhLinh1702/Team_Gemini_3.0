package auction.server.model;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;
    protected int id;
    private static int nextId = 1;

    public User(String username, String password, String role) {
        this.id = nextId++;
        this.username = username;
        this.password = password;
        this.role = role;
    }
    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getRole() {
        return role;
    }
}

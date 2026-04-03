package auction.server.model;

public class Bidder extends User {
    private double balance;
    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
        this.balance = 0.0;
    }
}
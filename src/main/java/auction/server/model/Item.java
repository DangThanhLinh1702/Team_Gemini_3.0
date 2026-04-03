package auction.server.model;

import java.util.UUID;

public class Item extends Entity{
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUserName;

    public Item( String name, String description, double startingPrice, String sellerUserName) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerUserName = sellerUserName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getSellerUserName() {
        return sellerUserName;
    }
}
